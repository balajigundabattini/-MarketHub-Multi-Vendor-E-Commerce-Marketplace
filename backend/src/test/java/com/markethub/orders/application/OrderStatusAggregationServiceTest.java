
package com.markethub.orders.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderEntity;

class OrderStatusAggregationServiceTest {

        @Test
        void shouldReturnCancelledWhenAllItemsAreCancelled() {
                assertEquals("CANCELLED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("CANCELLED", "CANCELLED")));
        }

        @Test
        void shouldReturnReturnedWhenAllItemsAreReturned() {
                assertEquals("RETURNED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("RETURNED", "RETURNED")));
        }

        @Test
        void shouldReturnPartiallyReturnedWhenSomeItemsAreReturned() {
                assertEquals("PARTIALLY_RETURNED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("RETURNED", "DELIVERED")));
        }

        @Test
        void shouldReturnPartiallyCancelledWhenSomeItemsAreCancelled() {
                assertEquals("PARTIALLY_CANCELLED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("CANCELLED", "ACTIVE")));
        }

        @Test
        void shouldReturnDeliveredWhenAllItemsAreDelivered() {
                assertEquals("DELIVERED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("DELIVERED", "DELIVERED")));
        }

        @Test
        void shouldReturnShippedWhenAllItemsAreShipped() {
                assertEquals("SHIPPED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("SHIPPED", "SHIPPED")));
        }

        @Test
        void shouldReturnPartiallyShippedWhenSomeItemsAreShipped() {
                assertEquals("PARTIALLY_SHIPPED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("SHIPPED", "ACTIVE")));
        }

        @Test
        void shouldReturnConfirmedWhenItemsAreNotYetShipped() {
                assertEquals("CONFIRMED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("ACTIVE", "ACTIVE")));
        }

        @Test
        void shouldRejectEmptyStatuses() {
                assertThrows(IllegalArgumentException.class,
                                () -> OrderStatusAggregationService.calculateStatus(List.of()));
        }

        @Test
        void shouldRejectUnknownStatus() {
                assertThrows(IllegalArgumentException.class,
                                () -> OrderStatusAggregationService.calculateStatus(
                                                List.of("ACTIVE", "DELIVERD")));
        }

        @Test
        void shouldRejectNullStatus() {
                assertThrows(IllegalArgumentException.class,
                                () -> OrderStatusAggregationService.calculateStatus(
                                                java.util.Arrays.asList("ACTIVE", null)));
        }

        @Test
        void shouldReturnPartiallyCompletedWhenItemsAreReturnedAndCancelled() {
                assertEquals("PARTIALLY_COMPLETED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("RETURNED", "CANCELLED")));
        }

        @Test
        void shouldReturnPartiallyCompletedWhenMultipleItemsHaveMixedFinalOutcomes() {
                assertEquals("PARTIALLY_COMPLETED",
                                OrderStatusAggregationService.calculateStatus(
                                                List.of("RETURNED", "CANCELLED", "RETURNED")));
        }

        @Test
        void shouldRecalculateParentOrderAndSaveHistory() {

                OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);

                OrderStatusAggregationService service = new OrderStatusAggregationService(history);

                OrderEntity order = mock(OrderEntity.class);
                SellerOrderEntity sellerOrder = mock(SellerOrderEntity.class);

                OrderItemEntity returnedItem = mock(OrderItemEntity.class);
                OrderItemEntity cancelledItem = mock(OrderItemEntity.class);

                when(order.getStatus()).thenReturn("CONFIRMED");
                when(order.getSellerOrders()).thenReturn(List.of(sellerOrder));
                when(sellerOrder.getItems())
                                .thenReturn(List.of(returnedItem, cancelledItem));

                when(returnedItem.getStatus()).thenReturn("RETURNED");
                when(cancelledItem.getStatus()).thenReturn("CANCELLED");

                service.recalculate(order);

                verify(order).setStatus("PARTIALLY_COMPLETED");

                verify(history).save(any(OrderStatusHistoryEntity.class));
        }

        @Test
        void shouldNotOverwriteProtectedOrderStatus() {

                OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);

                OrderStatusAggregationService service = new OrderStatusAggregationService(history);

                OrderEntity order = mock(OrderEntity.class);

                when(order.getStatus()).thenReturn("PAYMENT_EXPIRED");

                service.recalculate(order);

                verify(order, never()).setStatus(anyString());

                verify(history, never()).save(any(OrderStatusHistoryEntity.class));
        }

        @Test
        void shouldNotSaveHistoryWhenStatusDoesNotChange() {

                OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);

                OrderStatusAggregationService service = new OrderStatusAggregationService(history);

                OrderEntity order = mock(OrderEntity.class);
                SellerOrderEntity sellerOrder = mock(SellerOrderEntity.class);

                OrderItemEntity item1 = mock(OrderItemEntity.class);
                OrderItemEntity item2 = mock(OrderItemEntity.class);

                when(order.getStatus()).thenReturn("CONFIRMED");
                when(order.getSellerOrders()).thenReturn(List.of(sellerOrder));
                when(sellerOrder.getItems()).thenReturn(List.of(item1, item2));

                when(item1.getStatus()).thenReturn("ACTIVE");
                when(item2.getStatus()).thenReturn("ACTIVE");

                service.recalculate(order);

                verify(order, never()).setStatus(anyString());

                verify(history, never()).save(any(OrderStatusHistoryEntity.class));
        }

}
