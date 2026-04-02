# 07 - React Frontend Integration

## 1. Integration Goal

Provide a production-ready request layer for React clients consuming all non-auth API routes, including JSON and multipart/binary flows.

## 2. Environment Setup

Frontend `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 3. API Client Baseline

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## 4. Strongly Typed Contracts

```ts
export type ProjectStatus = "DRAFT" | "SHARED" | "ARCHIVED";

export interface ProjectResponse {
  id: number;
  name: string;
  status: ProjectStatus;
  timelineData: string;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationResponse {
  id: number;
  type: string;
  message: string;
  read: boolean;
  projectId: number;
  createdAt: string;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  errors?: Array<{ field: string; message: string }>;
}
```

## 5. Endpoint Call Layer

### Projects

```ts
export const createProject = (payload: { name: string; timelineData?: string }) =>
  api.post<ProjectResponse>("/projects", payload).then((r) => r.data);

export const getProject = (id: number) =>
  api.get<ProjectResponse>(`/projects/${id}`).then((r) => r.data);

export const listProjects = (page = 0, size = 10) =>
  api.get("/projects", { params: { page, size } }).then((r) => r.data);

export const listSharedProjects = (page = 0, size = 10) =>
  api.get("/projects/shared", { params: { page, size } }).then((r) => r.data);

export const updateProject = (
  id: number,
  payload: Partial<{ name: string; description: string; timelineData: string; status: ProjectStatus }>
) => api.patch<ProjectResponse>(`/projects/${id}`, payload).then((r) => r.data);

export const deleteProject = (id: number) => api.delete(`/projects/${id}`);

export const shareProject = (id: number, sharedWithEmail: string) =>
  api.post(`/projects/${id}/share`, { sharedWithEmail });
```

### History

```ts
export const getProjectHistory = (projectId: number) =>
  api.get(`/history/${projectId}`).then((r) => r.data);
```

### Notifications

```ts
export const getNotifications = () =>
  api.get<NotificationResponse[]>("/notifications").then((r) => r.data);

export const getUnreadNotifications = () =>
  api.get<NotificationResponse[]>("/notifications/unread").then((r) => r.data);

export const markNotificationRead = (id: number) =>
  api.patch(`/notifications/${id}/read`);
```

### Admin reports

```ts
export const getAdminReport = (format: "JSON" | "CSV" | "SUMMARY" = "JSON") =>
  api.get("/admin/reports", { params: { format } }).then((r) => r.data);
```

### AI multipart routes

```ts
export async function cleanAudio(file: File, options?: { backend?: string; stationary?: boolean; targetSr?: number }) {
  const form = new FormData();
  form.append("file", file);
  if (options?.backend) form.append("backend", options.backend);
  if (options?.stationary !== undefined) form.append("stationary", String(options.stationary));
  if (options?.targetSr !== undefined) form.append("targetSr", String(options.targetSr));

  const response = await api.post("/ai/audio/clean", form, {
    headers: { "Content-Type": "multipart/form-data" },
    responseType: "blob"
  });

  return response.data as Blob;
}

export async function transcribeAudio(file: File, options?: { model?: string; lang?: string; formats?: string[] }) {
  const form = new FormData();
  form.append("file", file);
  if (options?.model) form.append("model", options.model);
  if (options?.lang) form.append("lang", options.lang);
  options?.formats?.forEach((f) => form.append("formats", f));

  const response = await api.post("/ai/audio/transcribe", form, {
    headers: { "Content-Type": "multipart/form-data" },
    responseType: "blob"
  });

  return response.data as Blob;
}
```

## 6. Error Handling Standardization

```ts
import axios from "axios";

export function toUserMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ErrorResponse | undefined;
    if (data?.errors?.length) {
      return data.errors.map((e) => `${e.field}: ${e.message}`).join(" | ");
    }
    return data?.message || "Request failed";
  }
  return "Unexpected error";
}
```

## 7. Frontend Architecture Suggestions

- Keep API logic in a dedicated data-access layer.
- Use React Query (or similar) for request lifecycle and caching.
- Keep binary response handling in utility helpers.
- Use optimistic update for notification read state and rollback on failure.
