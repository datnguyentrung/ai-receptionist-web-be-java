package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CoursePriceMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CoursePriceRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CoursePriceServiceTest {
    @Test
    void deletesUnusedPrice() {
        CoursePriceRepository repository = mock(CoursePriceRepository.class);
        CoursePurchaseRepository purchases = mock(CoursePurchaseRepository.class);
        CoursePriceService service = service(repository, purchases);
        UUID priceId = UUID.randomUUID();
        CoursePrice price = CoursePrice.builder().coursePriceId(priceId).build();
        when(repository.findById(priceId)).thenReturn(Optional.of(price));
        when(purchases.existsByCoursePrice_CoursePriceId(priceId)).thenReturn(false);

        service.delete(priceId);

        verify(repository).delete(price);
    }

    @Test
    void rejectsDeletionWhenPriceHasPurchases() {
        CoursePriceRepository repository = mock(CoursePriceRepository.class);
        CoursePurchaseRepository purchases = mock(CoursePurchaseRepository.class);
        CoursePriceService service = service(repository, purchases);
        UUID priceId = UUID.randomUUID();
        CoursePrice price = CoursePrice.builder().coursePriceId(priceId).build();
        when(repository.findById(priceId)).thenReturn(Optional.of(price));
        when(purchases.existsByCoursePrice_CoursePriceId(priceId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(priceId))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(CatalogErrorCode.COURSE_PRICE_IN_USE));
        verify(repository, never()).delete(any());
    }

    @Test
    void rejectsDeletionWhenPriceDoesNotExist() {
        CoursePriceRepository repository = mock(CoursePriceRepository.class);
        CoursePurchaseRepository purchases = mock(CoursePurchaseRepository.class);
        CoursePriceService service = service(repository, purchases);
        UUID priceId = UUID.randomUUID();
        when(repository.findById(priceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(priceId))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(CatalogErrorCode.COURSE_PRICE_NOT_FOUND));
        verifyNoInteractions(purchases);
    }

    private CoursePriceService service(CoursePriceRepository repository, CoursePurchaseRepository purchases) {
        return new CoursePriceService(repository, mock(CoursePriceMapper.class), mock(CourseRepository.class), purchases);
    }
}
