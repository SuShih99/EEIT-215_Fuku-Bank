package com.javaeasybank.creditcard.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.javaeasybank.creditcard.dto.CardTypeResponseDto;
import com.javaeasybank.creditcard.service.CardTypeService;

@ExtendWith(MockitoExtension.class)
class CardTypeControllerRouteTest {

    @Mock
    private CardTypeService cardTypeService;

    @Test
    @DisplayName("public card type route returns card type list")
    void publicCardTypeRouteReturnsList() throws Exception {
        CardTypeResponseDto cardType = new CardTypeResponseDto();
        cardType.setCardTypeId(1);
        cardType.setCardTypeName("現金回饋卡");

        when(cardTypeService.findAll()).thenReturn(List.of(cardType));

        mockMvc().perform(get("/api/public/card-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].cardTypeName").value("現金回饋卡"));
    }

    @Test
    @DisplayName("user card type page route is not handled by card type API")
    void userCardTypeRouteIsNotCardTypeApi() throws Exception {
        mockMvc().perform(get("/user/card-types"))
                .andExpect(status().isNotFound());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new CardTypeController(cardTypeService)).build();
    }
}
