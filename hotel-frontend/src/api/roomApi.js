import { fetchClient } from "./apiClient";

const BASE_URL = "/api/rooms";

export const roomApi = {
  getAllRooms() {
    return fetchClient(BASE_URL, { method: "GET" });
  },

  getRoomById(id) {
    return fetchClient(`${BASE_URL}/${id}`, { method: "GET" });
  },

  getRoomsByFloor(floor) {
    return fetchClient(`${BASE_URL}/floor/${floor}`, { method: "GET" });
  },

  getRoomByRoomNumber(roomNumber) {
    return fetchClient(`${BASE_URL}/number/${roomNumber}`, { method: "GET" });
  },

  createRoom(data) {
    return fetchClient(BASE_URL, {
      method: "POST",
      body: data,
    });
  },

  updateRoom(id, data) {
    return fetchClient(`${BASE_URL}/${id}`, {
      method: "PUT",
      body: data,
    });
  },

  deleteRoom(id) {
    return fetchClient(`${BASE_URL}/${id}`, { method: "DELETE" });
  },
};
