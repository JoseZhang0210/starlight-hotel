import { createRouter, createWebHistory } from "vue-router";

import MainLayout from "../layouts/MainLayout.vue";
import AdminLayout from "../layouts/AdminLayout.vue";
import DashboardView from "../views/DashboardView.vue";
import HomeView from "../views/HomeView.vue";
import MemberManageView from "../views/MemberManageView.vue";
import EmployeeManageView from "../views/EmployeeManageView.vue";
//--------------------餐廳後台管理-----------------------------------
import RestaurantMenuView from "../views/RestaurantMenuView.vue";
import RestaurantManageView from "../views/RestaurantManageView.vue";
import RestaurantTimeManageView from "../views/RestaurantTimeManageView.vue";
import ReservationManageView from "../views/ReservationManageView.vue";
//--------------------餐廳前台管理-----------------------------------
import RestaurantReservationView from "../views/RestaurantReservationView.vue";
import LoginView from "../views/LoginView.vue";
import LogoutView from "../views/LogoutView.vue";
import RegisterView from "../views/RegisterView.vue";
import ForgotPasswordView from "../views/ForgotPasswordView.vue";
//---------------------- 商品後台管理-----------------
import ProductManageView from "../views/ProductManageView.vue";
import ProductEditView from "../views/ProductEditView.vue";
import ProductAddView from "../views/ProductAddView.vue";
import ProductShopView from "../views/ProductShopView.vue";
import ProductDetailView from "../views/ProductDetailView.vue";
//---------------------- 訂單後台管理 -----------------
import AdminOrdersView from "../views/AdminOrdersView.vue";
//---------------------------------------------------
import RentalView from "../views/RentalView.vue";
import AdminRentalView from "../views/AdminRentalView.vue"; // 後台獨立使用既有租借管理功能。
import VenueView from "../views/VenueView.vue";
//---------------------------------------------------
import AdminRoomTypeView from "../views/AdminRoomTypeView.vue";
import AdminRoomView from "../views/AdminRoomView.vue";
import AdminRoomImageView from "../views/AdminRoomImageView.vue";
import AdminRoomTaskView from "../views/AdminRoomTaskView.vue";
import AdminRoomBookingView from "../views/AdminRoomBookingView.vue";
import AdminRoomBookingPaymentView from "../views/AdminRoomBookingPaymentView.vue";
import AdminRoomAccessMonitorView from "../views/AdminRoomAccessMonitorView.vue";

// ---------------購物車 / 結帳 / 付款-------------------
import CartView from "../views/CartView.vue";
import CheckoutView from "../views/CheckoutView.vue";
import PaymentView from "../views/PaymentView.vue";
// ---------------優惠券-------------------
import AdminCouponsView from "@/views/AdminCouponsView.vue";

//--------------- 會員中心 -----------------
import MemberLayout from "../layouts/MemberLayout.vue";
import MemberProfileView from "../views/MemberProfileView.vue";
import MemberPasswordView from "../views/MemberPasswordView.vue";
import MyOrdersView from "../views/MyOrdersView.vue";
import ProductWishlistView from "../views/ProductWishlistView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    {
      path: "/mobile-pass",
      name: "mobile-pass",
      component: () => import("../views/MobilePassView.vue"),
    },
    {
      path: "/admin",
      component: AdminLayout,
      meta: { requiresAuth: true, requiresEmployee: true },
      children: [
        {
          path: "",
          name: "dashboard",
          component: DashboardView,
        },
        {
          path: "members",
          name: "admin-members",
          component: MemberManageView,
          meta: { permission: "MEMBER_MANAGE" },
        },
        {
          path: "employees",
          name: "admin-employees",
          component: EmployeeManageView,
          meta: { permission: "EMPLOYEE_MANAGE" },
        },
        {
          path: "restaurants",
          name: "admin-restaurants",
          component: RestaurantManageView,
          meta: { permission: "RESTAURANT_MANAGE" },
        },
        {
          path: "products",
          name: "admin-products",
          component: ProductManageView,
          meta: { permission: "PRODUCT_MANAGE" },
        },
        {
          path: "products/:id/edit",
          name: "admin-product-edit",
          component: ProductEditView,
          meta: { permission: "PRODUCT_MANAGE" },
        },
        {
          path: "products/add",
          name: "admin-product-add",
          component: ProductAddView,
          meta: { permission: "PRODUCT_MANAGE" },
        },
        {
          path: "orders",
          name: "admin-orders",
          component: AdminOrdersView,
          meta: { permission: "ORDER_MANAGE" },
        },
        {
          path: "coupons",
          name: "admin-coupons",
          component: AdminCouponsView,
          meta: { permission: "COUPON_MANAGE" },
        },
        {
          path: "restaurant-times",
          name: "admin-restaurant-times",
          component: RestaurantTimeManageView,
          meta: { permission: "RESTAURANT_MANAGE" },
        },
        {
          path: "reservations",
          name: "admin-reservations",
          component: ReservationManageView,
          meta: { permission: "RESTAURANT_MANAGE" },
        },
        // ==========訂房==========

        {
          path: "room-status",
          name: "admin-room-status",
          component: AdminRoomView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "room-types",
          name: "admin-room-types",
          component: AdminRoomTypeView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "room-images",
          name: "admin-room-images",
          component: AdminRoomImageView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "room-task",
          name: "admin-room-task",
          component: AdminRoomTaskView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "room-booking",
          name: "admin-room-booking",
          component: AdminRoomBookingView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "booking-payments",
          name: "admin-booking-payments",
          component: AdminRoomBookingPaymentView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        {
          path: "room-access-monitor",
          name: "admin-room-access-monitor",
          component: AdminRoomAccessMonitorView,
          meta: { permissions: ["ROOM_MANAGE", "BOOKING_MANAGE"] },
        },
        //===============================
        {
          path: "venues",
          name: "admin-venues",
          component: VenueView,
          meta: { permission: "VENUE_MANAGE" },
        },

        {
          path: "rental",
          name: "admin-rental",
          component: AdminRentalView, // 管理列表不再進入會員專用元件。
          meta: { permission: "VENUE_MANAGE" },
        },
      ],
    },

    {
      path: "/",
      component: MainLayout,
      children: [
        {
          path: "",
          name: "home",
          component: HomeView,
        },
        {
          path: "/login",
          name: "login",
          component: LoginView,
        },
        {
          path: "/logout",
          name: "logout",
          component: LogoutView,
        },
        {
          path: "/register",
          name: "register",
          component: RegisterView,
        },
        {
          path: "/forgot-password",
          name: "forgot-password",
          component: ForgotPasswordView,
        },
        {
          path: "products",
          name: "product-shop",
          component: ProductShopView,
        },
        {
          path: "products/:id",
          name: "product-detail",
          component: ProductDetailView,
        },
        {
          path: "cart",
          name: "cart",
          component: CartView,
        },
        {
          path: "checkout",
          name: "checkout",
          component: CheckoutView,
        },
        {
          path: "payment/:orderId",
          name: "payment",
          component: PaymentView,
        },
        {
          path: "restaurant-menu",
          name: "restaurant-menu",
          component: RestaurantMenuView,
        },
        {
          path: "restaurants",
          name: "restaurants",
          component: RestaurantManageView,
        },
        {
          path: "restaurant-reservation",
          name: "restaurant-reservation",
          component: RestaurantReservationView,
        },
        {
          path: "restaurant-times",
          name: "restaurant-times",
          component: RestaurantTimeManageView,
        },
        {
          path: "reservation-manage",
          name: "reservation-manage",
          component: ReservationManageView,
        },
        {
          path: "rentals",
          name: "rentals",
          component: RentalView,
        },
        {
          path: "about",
          name: "about",
          component: () => import("../views/AboutView.vue"),
        },
        {
          path: "/room-booking",
          name: "room-booking",
          component: () => import("../views/RoomBookingView.vue"),
        },
        {
          path: "/room-selection",
          name: "room-selection",
          component: () => import("../views/RoomSelectionView.vue"),
        },
        {
          path: "/room-checkout",
          name: "room-checkout",
          component: () => import("../views/RoomCheckoutView.vue"),
        },
        {
          path: "my-orders",
          name: "my-orders",
          component: MyOrdersView,
        },
        {
          path: "member",
          component: MemberLayout,
          children: [
            {
              path: "",
              name: "member-center",
              component: MemberProfileView,
            },
            {
              path: "profile",
              name: "member-profile",
              component: MemberProfileView,
            },
            {
              path: "password",
              name: "member-password",
              component: MemberPasswordView,
            },
            {
              path: "orders",
              name: "member-orders",
              component: MyOrdersView,
            },
            {
              path: "wishlist",
              name: "member-wishlist",
              component: ProductWishlistView,
            },
            {
              path: "room-bookings",
              name: "member-room-bookings",
              component: () => import("../views/MemberRoomBookingView.vue"),
            },
            {
              // 新增會員專屬場地預約入口。
              path: "rentals", // 完整網址為 /member/rentals。
              name: "member-rentals", // 提供場地頁面辨識會員模式。
              component: RentalView, // 重用場地頁面並由後端限制本人資料。
            }, // 結束場地預約路由。
          ],
        },
      ],
    },
  ],
  //==========跳傳至最上方=====================
  scrollBehavior() {
    return { top: 0 };
  },
});

import { useAuthStore } from "@/stores/auth";

router.beforeEach(async (to, from, next) => {
  try {
    const authStore = useAuthStore();
    const token = localStorage.getItem("token");
    if (token) {
      await authStore.checkAndRefreshToken();
    }

    const isAdminRoute =
      to.matched.some((record) => record.meta?.requiresEmployee) ||
      to.path.startsWith("/admin");

    if (isAdminRoute) {
      // 1. 檢查是否已登入
      if (!authStore.isLoggedIn) {
        return next({
          name: "login",
          query: { redirect: to.fullPath },
        });
      }

      // 2. 檢查是否具備員工身分 (ROLE_EMPLOYEE / POSITION_*)
      if (!authStore.isEmployee) {
        console.warn("非員工身分嘗試進入後台，拒絕訪問");
        return next({ name: "home" });
      }

      // 3. 檢查細部功能權限
      const requiredPermission = to.meta?.permission;
      const requiredPermissions = to.meta?.permissions;

      if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
        console.warn(
          `缺乏指定後台權限 [${requiredPermission}]，重定向至後台首頁`,
        );
        if (to.path !== "/admin") {
          return next({ path: "/admin" });
        }
      }

      if (
        requiredPermissions &&
        !authStore.hasAnyPermission(requiredPermissions)
      ) {
        console.warn(
          `缺乏指定後台權限清單 [${requiredPermissions.join(", ")}]，重定向至後台首頁`,
        );
        if (to.path !== "/admin") {
          return next({ path: "/admin" });
        }
      }
    }
  } catch (e) {
    console.error("Route auth check error:", e);
  }
  next();
});

export default router;
