package com.hotel.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotel.model.entity.Venue;
import com.hotel.repository.RentalRepository;
import com.hotel.repository.VenueRepository;

/**
 * 場地更新的回歸測試。
 */
@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

        @Mock
        private VenueRepository venueRepository;

        @Mock
        private RentalRepository rentalRepository;

        private VenueService venueService;

        /** 為每個測試建立乾淨的場地服務。 */
        @BeforeEach
        void setUp() {
                venueService = new VenueService(
                                venueRepository,
                                rentalRepository); // 每個測試使用乾淨的場地服務。
        }

        /** 驗證本機上傳路徑可正常保存。 */
        @Test
        void saveAcceptsLocalUploadPathWithoutNullSchemeFailure() {
                Venue venue = venue(
                                1,
                                50,
                                "/uploads/venues/venue-1.jpg");

                when(venueRepository.save(venue))
                                .thenReturn(venue); // 模擬新增流程寫入場地。

                Venue saved = assertDoesNotThrow(
                                () -> venueService.save(venue)); // 相對路徑不應再觸發空 scheme 例外。

                assertEquals(
                                "/uploads/venues/venue-1.jpg",
                                saved.getImageUrl());
                verify(venueRepository).save(venue);
        }

        /** 驗證更新僅複製允許修改的場地欄位。 */
        @Test
        void updateExistingCopiesAllowedFieldsToManagedVenue() {
                Venue managed = venue(
                                1,
                                50,
                                "/uploads/venues/original.jpg");
                Venue incoming = venue(
                                1,
                                49,
                                "/uploads/venues/replacement.jpg");

                when(venueRepository.findById(1))
                                .thenReturn(Optional.of(managed)); // 回傳目前交易中的既有場地。

                Venue updated = venueService.updateExisting(
                                incoming); // 只更新允許修改的 Venue 欄位。

                assertEquals(1, updated.getVenueId());
                assertEquals(49, updated.getCapacity());
                assertEquals(
                                "/uploads/venues/replacement.jpg",
                                updated.getImageUrl());
                verify(venueRepository).findById(1);
        }

        /** 驗證固定場地容量限制與 13_venues.json 一致。 */
        @Test
        void fixedVenueCapacitiesRejectValuesAboveConfiguredMaximum() {
                int[][] boundaryCases = {
                                { 1, 200 },
                                { 2, 150 },
                                { 3, 60 },
                                { 4, 100 }
                }; // 依 13_venues.json：1=200, 2=150, 3=60, 4=100

                for (int[] boundaryCase : boundaryCases) {
                        int venueId = boundaryCase[0];
                        int capacityLimit = boundaryCase[1];
                        Venue incoming = venue(
                                        venueId,
                                        capacityLimit + 1,
                                        "/images/venues/venue-" + venueId + ".jpg");

                        IllegalArgumentException exception = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> venueService.updateExisting(incoming));

                        assertEquals(
                                        "場地「宴會廳甲」容量不可超過 "
                                                        + capacityLimit
                                                        + " 人",
                                        exception.getMessage());
                }
        }

        /** 測試 ID 1~4 不可刪除。 */
        @Test
        void deleteById_defaultVenues1To4_throwsIllegalStateException() {
                for (int id = 1; id <= 4; id++) {
                        final int venueId = id;
                        IllegalStateException exception = assertThrows(
                                        IllegalStateException.class,
                                        () -> venueService.deleteById(venueId));
                        assertEquals("系統預設場地不可刪除，如暫停使用請將狀態改為維護中或停用", exception.getMessage());
                }
        }

        /** 測試 ID > 4 且無 rental 可維持既有刪除行為。 */
        @Test
        void deleteById_customVenueWithoutRental_deletesSuccessfully() {
                int id = 5;
                when(venueRepository.existsById(id)).thenReturn(true);
                when(rentalRepository.existsByVenueId(id)).thenReturn(false);

                boolean result = venueService.deleteById(id);

                assertTrue(result);
                verify(venueRepository).deleteById(id);
                verify(venueRepository).flush();
        }

        /** 測試 ID > 4 有 rental 仍不可刪。 */
        @Test
        void deleteById_customVenueWithRental_throwsIllegalStateException() {
                int id = 5;
                when(venueRepository.existsById(id)).thenReturn(true);
                when(rentalRepository.existsByVenueId(id)).thenReturn(true);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> venueService.deleteById(id));

                assertEquals("此場地已有租借紀錄，無法刪除", exception.getMessage());
                verify(venueRepository, never()).deleteById(id);
        }

        /**
         * 建立測試用場地資料。
         */
        private Venue venue(
                        int venueId,
                        int capacity,
                        String imageUrl) {

                Venue venue = new Venue();
                venue.setVenueId(venueId);
                venue.setVenueName("宴會廳甲");
                venue.setCapacity(capacity);
                venue.setPricePerDay(5000);
                venue.setVenueStatus("AVAILABLE");
                venue.setImageUrl(imageUrl);
                return venue;
        }
}
