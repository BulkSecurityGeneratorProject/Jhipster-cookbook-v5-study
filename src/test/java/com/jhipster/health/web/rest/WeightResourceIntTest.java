package com.jhipster.health.web.rest;

import com.jhipster.health.TwentyOnePointsApp;

import com.jhipster.health.domain.Weight;
import com.jhipster.health.repository.WeightRepository;
import com.jhipster.health.repository.search.WeightSearchRepository;
import com.jhipster.health.service.WeightService;
import com.jhipster.health.service.dto.WeightDTO;
import com.jhipster.health.service.mapper.WeightMapper;
import com.jhipster.health.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;


import static com.jhipster.health.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the WeightResource REST controller.
 *
 * @see WeightResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TwentyOnePointsApp.class)
public class WeightResourceIntTest {

    private static final LocalDate DEFAULT_TIMESTAMP = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TIMESTAMP = LocalDate.now(ZoneId.systemDefault());

    private static final Integer DEFAULT_WEIGHT = 1;
    private static final Integer UPDATED_WEIGHT = 2;

    @Autowired
    private WeightRepository weightRepository;

    @Autowired
    private WeightMapper weightMapper;

    @Autowired
    private WeightService weightService;

    /**
     * This repository is mocked in the com.jhipster.health.repository.search test package.
     *
     * @see com.jhipster.health.repository.search.WeightSearchRepositoryMockConfiguration
     */
    @Autowired
    private WeightSearchRepository mockWeightSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restWeightMockMvc;

    private Weight weight;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final WeightResource weightResource = new WeightResource(weightService);
        this.restWeightMockMvc = MockMvcBuilders.standaloneSetup(weightResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Weight createEntity(EntityManager em) {
        Weight weight = new Weight()
            .timestamp(DEFAULT_TIMESTAMP)
            .weight(DEFAULT_WEIGHT);
        return weight;
    }

    @Before
    public void initTest() {
        weight = createEntity(em);
    }

    @Test
    @Transactional
    public void createWeight() throws Exception {
        int databaseSizeBeforeCreate = weightRepository.findAll().size();

        // Create the Weight
        WeightDTO weightDTO = weightMapper.toDto(weight);
        restWeightMockMvc.perform(post("/api/weights")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(weightDTO)))
            .andExpect(status().isCreated());

        // Validate the Weight in the database
        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeCreate + 1);
        Weight testWeight = weightList.get(weightList.size() - 1);
        assertThat(testWeight.getTimestamp()).isEqualTo(DEFAULT_TIMESTAMP);
        assertThat(testWeight.getWeight()).isEqualTo(DEFAULT_WEIGHT);

        // Validate the Weight in Elasticsearch
        verify(mockWeightSearchRepository, times(1)).save(testWeight);
    }

    @Test
    @Transactional
    public void createWeightWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = weightRepository.findAll().size();

        // Create the Weight with an existing ID
        weight.setId(1L);
        WeightDTO weightDTO = weightMapper.toDto(weight);

        // An entity with an existing ID cannot be created, so this API call must fail
        restWeightMockMvc.perform(post("/api/weights")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(weightDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Weight in the database
        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeCreate);

        // Validate the Weight in Elasticsearch
        verify(mockWeightSearchRepository, times(0)).save(weight);
    }

    @Test
    @Transactional
    public void checkTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = weightRepository.findAll().size();
        // set the field null
        weight.setTimestamp(null);

        // Create the Weight, which fails.
        WeightDTO weightDTO = weightMapper.toDto(weight);

        restWeightMockMvc.perform(post("/api/weights")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(weightDTO)))
            .andExpect(status().isBadRequest());

        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllWeights() throws Exception {
        // Initialize the database
        weightRepository.saveAndFlush(weight);

        // Get all the weightList
        restWeightMockMvc.perform(get("/api/weights?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(weight.getId().intValue())))
            .andExpect(jsonPath("$.[*].timestamp").value(hasItem(DEFAULT_TIMESTAMP.toString())))
            .andExpect(jsonPath("$.[*].weight").value(hasItem(DEFAULT_WEIGHT)));
    }
    
    @Test
    @Transactional
    public void getWeight() throws Exception {
        // Initialize the database
        weightRepository.saveAndFlush(weight);

        // Get the weight
        restWeightMockMvc.perform(get("/api/weights/{id}", weight.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(weight.getId().intValue()))
            .andExpect(jsonPath("$.timestamp").value(DEFAULT_TIMESTAMP.toString()))
            .andExpect(jsonPath("$.weight").value(DEFAULT_WEIGHT));
    }

    @Test
    @Transactional
    public void getNonExistingWeight() throws Exception {
        // Get the weight
        restWeightMockMvc.perform(get("/api/weights/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateWeight() throws Exception {
        // Initialize the database
        weightRepository.saveAndFlush(weight);

        int databaseSizeBeforeUpdate = weightRepository.findAll().size();

        // Update the weight
        Weight updatedWeight = weightRepository.findById(weight.getId()).get();
        // Disconnect from session so that the updates on updatedWeight are not directly saved in db
        em.detach(updatedWeight);
        updatedWeight
            .timestamp(UPDATED_TIMESTAMP)
            .weight(UPDATED_WEIGHT);
        WeightDTO weightDTO = weightMapper.toDto(updatedWeight);

        restWeightMockMvc.perform(put("/api/weights")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(weightDTO)))
            .andExpect(status().isOk());

        // Validate the Weight in the database
        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeUpdate);
        Weight testWeight = weightList.get(weightList.size() - 1);
        assertThat(testWeight.getTimestamp()).isEqualTo(UPDATED_TIMESTAMP);
        assertThat(testWeight.getWeight()).isEqualTo(UPDATED_WEIGHT);

        // Validate the Weight in Elasticsearch
        verify(mockWeightSearchRepository, times(1)).save(testWeight);
    }

    @Test
    @Transactional
    public void updateNonExistingWeight() throws Exception {
        int databaseSizeBeforeUpdate = weightRepository.findAll().size();

        // Create the Weight
        WeightDTO weightDTO = weightMapper.toDto(weight);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWeightMockMvc.perform(put("/api/weights")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(weightDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Weight in the database
        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Weight in Elasticsearch
        verify(mockWeightSearchRepository, times(0)).save(weight);
    }

    @Test
    @Transactional
    public void deleteWeight() throws Exception {
        // Initialize the database
        weightRepository.saveAndFlush(weight);

        int databaseSizeBeforeDelete = weightRepository.findAll().size();

        // Delete the weight
        restWeightMockMvc.perform(delete("/api/weights/{id}", weight.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Weight> weightList = weightRepository.findAll();
        assertThat(weightList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Weight in Elasticsearch
        verify(mockWeightSearchRepository, times(1)).deleteById(weight.getId());
    }

    @Test
    @Transactional
    public void searchWeight() throws Exception {
        // Initialize the database
        weightRepository.saveAndFlush(weight);
        when(mockWeightSearchRepository.search(queryStringQuery("id:" + weight.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(weight), PageRequest.of(0, 1), 1));
        // Search the weight
        restWeightMockMvc.perform(get("/api/_search/weights?query=id:" + weight.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(weight.getId().intValue())))
            .andExpect(jsonPath("$.[*].timestamp").value(hasItem(DEFAULT_TIMESTAMP.toString())))
            .andExpect(jsonPath("$.[*].weight").value(hasItem(DEFAULT_WEIGHT)));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Weight.class);
        Weight weight1 = new Weight();
        weight1.setId(1L);
        Weight weight2 = new Weight();
        weight2.setId(weight1.getId());
        assertThat(weight1).isEqualTo(weight2);
        weight2.setId(2L);
        assertThat(weight1).isNotEqualTo(weight2);
        weight1.setId(null);
        assertThat(weight1).isNotEqualTo(weight2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(WeightDTO.class);
        WeightDTO weightDTO1 = new WeightDTO();
        weightDTO1.setId(1L);
        WeightDTO weightDTO2 = new WeightDTO();
        assertThat(weightDTO1).isNotEqualTo(weightDTO2);
        weightDTO2.setId(weightDTO1.getId());
        assertThat(weightDTO1).isEqualTo(weightDTO2);
        weightDTO2.setId(2L);
        assertThat(weightDTO1).isNotEqualTo(weightDTO2);
        weightDTO1.setId(null);
        assertThat(weightDTO1).isNotEqualTo(weightDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(weightMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(weightMapper.fromId(null)).isNull();
    }
}
