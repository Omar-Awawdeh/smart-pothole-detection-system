# Web Dashboard Implementation Plan

**Owner**: Hamza  
**Duration**: Weeks 2-3 (Days 8-21, parallel with backend)  
**Stack**: Astro + React + Tailwind CSS + Leaflet

---

## Objective

Build an admin dashboard that:
1. Displays pothole locations on an interactive map
2. Lists potholes in a sortable/filterable table
3. Allows status updates (verify, mark repaired, flag false positive)
4. Manages vehicles and users
5. Shows statistics and analytics
6. Provides authentication for admin access

---

## Why Astro + React?

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| **Astro + React** | Fast static pages, small JS bundle, islands architecture | Learning curve for islands pattern | **This project** |
| Next.js | Full React, SSR/SSG, large ecosystem | Larger bundle, more complex | Complex apps |
| React + Vite | Simple setup, full SPA | No SSR, larger initial load | Simple SPAs |

**Decision**: Astro with React islands - most of our pages are static (layouts, navigation), only specific components (map, tables) need interactivity.

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                         DASHBOARD ARCHITECTURE                          │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        Astro Pages                                │  │
│  │  Static HTML shell, layouts, navigation, SEO                     │  │
│  │                                                                   │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐             │  │
│  │  │ index   │  │potholes │  │   map   │  │vehicles │  ...        │  │
│  │  │ .astro  │  │ .astro  │  │ .astro  │  │ .astro  │             │  │
│  │  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘             │  │
│  └───────┼────────────┼────────────┼────────────┼───────────────────┘  │
│          │            │            │            │                       │
│          ▼            ▼            ▼            ▼                       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    React Islands (client:load)                    │  │
│  │  Interactive components that need JavaScript                      │  │
│  │                                                                   │  │
│  │  ┌───────────┐  ┌────────────┐  ┌──────────┐  ┌──────────────┐  │  │
│  │  │ StatsCards│  │PotholeTable│  │ MapView  │  │ StatusSelect │  │  │
│  │  │  .tsx     │  │   .tsx     │  │   .tsx   │  │    .tsx      │  │  │
│  │  └───────────┘  └────────────┘  └──────────┘  └──────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                       Shared Utilities                            │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────────────┐ │  │
│  │  │  API    │  │  Auth   │  │  Types  │  │   Stores (nanostores)│ │  │
│  │  │ Client  │  │ Helpers │  │         │  │                     │ │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
dashboard/
├── src/
│   ├── pages/
│   │   ├── index.astro              # Dashboard home (redirects or overview)
│   │   ├── login.astro              # Login page
│   │   ├── dashboard.astro          # Main dashboard with stats
│   │   ├── potholes/
│   │   │   ├── index.astro          # Pothole list view
│   │   │   └── [id].astro           # Single pothole detail
│   │   ├── map.astro                # Full-page map view
│   │   ├── vehicles/
│   │   │   ├── index.astro          # Vehicle list
│   │   │   └── [id].astro           # Vehicle detail
│   │   ├── users/
│   │   │   └── index.astro          # User management (admin only)
│   │   └── settings.astro           # Settings page
│   │
│   ├── layouts/
│   │   ├── Layout.astro             # Base HTML layout
│   │   ├── DashboardLayout.astro    # Dashboard shell with sidebar
│   │   └── AuthLayout.astro         # Login page layout
│   │
│   ├── components/
│   │   ├── astro/                   # Static Astro components
│   │   │   ├── Sidebar.astro
│   │   │   ├── Header.astro
│   │   │   ├── Card.astro
│   │   │   ├── PageHeader.astro
│   │   │   └── Footer.astro
│   │   │
│   │   └── react/                   # Interactive React components
│   │       ├── PotholeMap.tsx       # Leaflet map
│   │       ├── PotholeTable.tsx     # TanStack Table
│   │       ├── StatsCards.tsx       # Dashboard stats
│   │       ├── StatusBadge.tsx      # Status indicator
│   │       ├── StatusSelect.tsx     # Status dropdown
│   │       ├── ConfirmDialog.tsx    # Confirmation modal
│   │       ├── DateRangePicker.tsx  # Date filter
│   │       ├── VehicleCard.tsx      # Vehicle display
│   │       └── LoginForm.tsx        # Login form
│   │
│   ├── lib/
│   │   ├── api.ts                   # API client (fetch wrapper)
│   │   ├── auth.ts                  # Auth utilities
│   │   ├── types.ts                 # TypeScript types
│   │   └── utils.ts                 # Helper functions
│   │
│   ├── stores/
│   │   └── auth.ts                  # Nanostores for auth state
│   │
│   └── styles/
│       └── global.css               # Tailwind imports + custom styles
│
├── public/
│   ├── favicon.svg
│   └── images/
│
├── astro.config.mjs
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

---

## Page Designs

### Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ┌──────┐                                           🔔  👤 Admin User  │
│  │ Logo │   Pothole AI                                                  │
├──┴──────┴───────────────────────────────────────────────────────────────┤
│  │                │                                                     │
│  │  📊 Dashboard  │   Dashboard Overview                               │
│  │  ───────────── │   ─────────────────                                │
│  │                │                                                     │
│  │  🕳️ Potholes   │   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐│
│  │                │   │ Total    │ │Unverified│ │ Verified │ │Repaired││
│  │  🗺️ Map View   │   │   847    │ │   234    │ │   412    │ │  201   ││
│  │                │   │ +12 today│ │ ⚠️ Action │ │ ✓ Good   │ │ 🔧 Done ││
│  │  🚗 Vehicles   │   └──────────┘ └──────────┘ └──────────┘ └────────┘│
│  │                │                                                     │
│  │  👥 Users      │   ┌─────────────────────────────────────────────────┤
│  │                │   │              Map Preview                        │
│  │  ⚙️ Settings   │   │   ┌─────────────────────────────────────────┐  │
│  │                │   │   │                                         │  │
│  │                │   │   │         [Interactive Map]               │  │
│  │                │   │   │         Recent Potholes                 │  │
│  │                │   │   │                                         │  │
│  │                │   │   └─────────────────────────────────────────┘  │
│  │                │   │                     [View Full Map →]          │
│  │                │   └─────────────────────────────────────────────────┤
│  │                │                                                     │
│  │                │   Recent Activity                                   │
│  │                │   ┌─────────────────────────────────────────────────┤
│  │                │   │ 🕳️ New pothole detected - Main St & 5th Ave   │
│  │                │   │ ✓ Pothole verified - Highway 101, Mile 45     │
│  │                │   │ 🔧 Pothole repaired - Oak Road, Block 12       │
│  │                │   └─────────────────────────────────────────────────┤
│  │                │                                                     │
└──┴────────────────┴─────────────────────────────────────────────────────┘
```

### Pothole List Page

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Sidebar │   Pothole Management                                         │
│          │   ─────────────────────                                      │
│          │                                                              │
│          │   ┌─────────────────────────────────────────────────────────┐│
│          │   │ Filters:                                                ││
│          │   │ [Status ▼] [Severity ▼] [Vehicle ▼] [Date Range 📅]    ││
│          │   │                                          [Search 🔍]    ││
│          │   └─────────────────────────────────────────────────────────┘│
│          │                                                              │
│          │   ┌─────────────────────────────────────────────────────────┐│
│          │   │ ID       │ Detected   │ Location      │ Conf │ Status  ││
│          │   │──────────│────────────│───────────────│──────│─────────││
│          │   │ PTH-1234 │ 2025-01-10 │ Main St & 5th │ 92%  │🟡Unverif││
│          │   │ PTH-1235 │ 2025-01-10 │ Oak Road      │ 87%  │🟢Verified│
│          │   │ PTH-1236 │ 2025-01-10 │ Highway 101   │ 95%  │🟡Unverif││
│          │   │ PTH-1237 │ 2025-01-09 │ Park Ave      │ 78%  │🔵Repaired│
│          │   │ PTH-1238 │ 2025-01-09 │ Industrial    │ 89%  │🟢Verified│
│          │   │ ...      │            │               │      │         ││
│          │   └─────────────────────────────────────────────────────────┘│
│          │                                                              │
│          │   Showing 1-20 of 847     [← Prev] [1] [2] [3] ... [Next →] │
│          │                                                              │
└──────────┴──────────────────────────────────────────────────────────────┘
```

### Map View Page

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Sidebar │   Map View                                    [Filters 🔽]   │
│          │   ────────                                                   │
│          │   ┌─────────────────────────────────────────────────────────┐│
│          │   │                                                         ││
│          │   │                                                         ││
│          │   │                    [Leaflet Map]                        ││
│          │   │                                                         ││
│          │   │         🔴 = Unverified    🟢 = Verified                ││
│          │   │         🔵 = Repaired      ⚫ = False Positive          ││
│          │   │                                                         ││
│          │   │    ┌─────────────────┐                                  ││
│          │   │    │ Pothole Details │   (popup on marker click)        ││
│          │   │    │ ID: PTH-1234    │                                  ││
│          │   │    │ Conf: 92%       │                                  ││
│          │   │    │ Status: Unverif │                                  ││
│          │   │    │ [View] [Update] │                                  ││
│          │   │    └─────────────────┘                                  ││
│          │   │                                                         ││
│          │   │                  [+] [-] [📍]                           ││
│          │   └─────────────────────────────────────────────────────────┘│
│          │                                                              │
│          │   Legend: 🔴 12 Unverified  🟢 45 Verified  🔵 23 Repaired  │
│          │                                                              │
└──────────┴──────────────────────────────────────────────────────────────┘
```

### Pothole Detail Page

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Sidebar │   Pothole PTH-1234                    [← Back to List]      │
│          │   ─────────────────                                          │
│          │                                                              │
│          │   ┌─────────────────┐  ┌────────────────────────────────────┐│
│          │   │                 │  │ Details                            ││
│          │   │                 │  │ ───────                            ││
│          │   │   [Pothole      │  │                                    ││
│          │   │    Image]       │  │ Status:   [Unverified ▼]          ││
│          │   │                 │  │ Severity: [High ▼]                 ││
│          │   │                 │  │                                    ││
│          │   │                 │  │ Detected: Jan 10, 2025 14:32       ││
│          │   └─────────────────┘  │ Vehicle:  VEH-001 (Ford Transit)   ││
│          │                        │ Confidence: 92%                    ││
│          │   ┌─────────────────┐  │ Confirmations: 3                   ││
│          │   │   [Mini Map]    │  │                                    ││
│          │   │   Location pin  │  │ Coordinates:                       ││
│          │   └─────────────────┘  │ 32.5521° N, 35.8461° E             ││
│          │                        │                                    ││
│          │   Actions:             │ [Save Changes]  [Delete]           ││
│          │   [✓ Verify]           └────────────────────────────────────┘│
│          │   [🔧 Mark Repaired]                                         │
│          │   [❌ False Positive]                                        │
│          │                                                              │
└──────────┴──────────────────────────────────────────────────────────────┘
```

---

## Component Specifications

### React Components

#### 1. PotholeMap.tsx

```
PURPOSE: Interactive map displaying pothole markers

PROPS:
  potholes: Pothole[]           # Array of potholes to display
  center?: [lat, lng]           # Initial center (default: Jordan)
  zoom?: number                 # Initial zoom level
  onMarkerClick?: (id) => void  # Callback when marker clicked
  selectedId?: string           # Highlighted pothole

FEATURES:
  - Leaflet map with OpenStreetMap tiles
  - Colored markers by status (red/green/blue/gray)
  - Marker clustering for performance
  - Popup on click with pothole summary
  - Fit bounds to show all markers
  - Current location button

LIBRARIES:
  - react-leaflet
  - leaflet
  - @react-leaflet/cluster (for marker clustering)

STATE:
  - Map instance ref
  - Selected marker
  - Popup content
```

#### 2. PotholeTable.tsx

```
PURPOSE: Sortable, filterable data table for potholes

PROPS:
  initialData?: Pothole[]       # Pre-loaded data (SSR)
  onRowClick?: (id) => void     # Row click handler

FEATURES:
  - Column sorting (click header)
  - Filtering by status, severity, date
  - Pagination (20 items per page)
  - Search by location/ID
  - Status badge with color
  - Quick actions (view, update status)
  - Responsive (horizontal scroll on mobile)

LIBRARIES:
  - @tanstack/react-table
  - Date formatting: date-fns

STATE:
  - Sorting state
  - Filter state
  - Pagination state
  - Data (fetched or passed)
```

#### 3. StatsCards.tsx

```
PURPOSE: Dashboard overview statistics

PROPS:
  stats: {
    total: number
    unverified: number
    verified: number
    repaired: number
    todayCount: number
  }

FEATURES:
  - Four stat cards in a row
  - Color-coded by status
  - Today's count badge
  - Loading skeleton
  - Animate on data change (optional)

STATE:
  - Loading state (if fetching)
```

#### 4. StatusSelect.tsx

```
PURPOSE: Dropdown to update pothole status

PROPS:
  currentStatus: Status
  onStatusChange: (newStatus) => Promise<void>
  disabled?: boolean

FEATURES:
  - Dropdown with status options
  - Color indicator for each option
  - Confirmation for destructive actions (false positive)
  - Loading state during update
  - Error handling with toast

STATE:
  - Open/closed
  - Loading
  - Selected value
```

#### 5. LoginForm.tsx

```
PURPOSE: Authentication form

PROPS:
  onSuccess: () => void        # Redirect after login

FEATURES:
  - Email + password fields
  - Validation (Zod + react-hook-form)
  - Error messages
  - Loading state
  - Remember me checkbox
  - Secure password field toggle

STATE:
  - Form values
  - Validation errors
  - Submitting state
  - Server error message
```

---

## API Client

```
API_CLIENT_DESIGN:

# Base configuration
BASE_URL = import.meta.env.PUBLIC_API_URL

# Token management
getToken() -> string | null:
    RETURN localStorage.getItem('accessToken')

setTokens(access, refresh):
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)

clearTokens():
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')

# Fetch wrapper with auth
async apiFetch(endpoint, options = {}):
    
    token = getToken()
    
    headers = {
        'Content-Type': 'application/json',
        ...options.headers
    }
    
    IF token:
        headers['Authorization'] = 'Bearer ' + token
    
    response = await fetch(BASE_URL + endpoint, {
        ...options,
        headers
    })
    
    IF response.status == 401:
        # Try refresh token
        refreshed = await refreshToken()
        IF refreshed:
            RETURN apiFetch(endpoint, options)  # Retry
        ELSE:
            clearTokens()
            window.location.href = '/login'
    
    IF NOT response.ok:
        error = await response.json()
        THROW new ApiError(error.message, response.status)
    
    RETURN response.json()

# API methods
api = {
    auth: {
        login: (email, password) => apiFetch('/auth/login', { 
            method: 'POST', 
            body: JSON.stringify({ email, password }) 
        }),
        logout: () => apiFetch('/auth/logout', { method: 'POST' }),
        me: () => apiFetch('/auth/me')
    },
    
    potholes: {
        list: (params) => apiFetch('/potholes?' + new URLSearchParams(params)),
        get: (id) => apiFetch('/potholes/' + id),
        update: (id, data) => apiFetch('/potholes/' + id, {
            method: 'PATCH',
            body: JSON.stringify(data)
        }),
        delete: (id) => apiFetch('/potholes/' + id, { method: 'DELETE' })
    },
    
    vehicles: {
        list: () => apiFetch('/vehicles'),
        get: (id) => apiFetch('/vehicles/' + id),
        create: (data) => apiFetch('/vehicles', {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        update: (id, data) => apiFetch('/vehicles/' + id, {
            method: 'PATCH',
            body: JSON.stringify(data)
        })
    },
    
    stats: {
        overview: () => apiFetch('/stats/overview'),
        daily: (days = 30) => apiFetch('/stats/daily?days=' + days)
    }
}
```

---

## Authentication Flow

```
AUTHENTICATION_FLOW:

1. User visits protected page
   │
   ▼
2. Check for token in localStorage
   │
   ├─► No token → Redirect to /login
   │
   └─► Has token → Validate token
       │
       ├─► Token valid → Show page
       │
       └─► Token expired → Try refresh
           │
           ├─► Refresh success → Update token, show page
           │
           └─► Refresh failed → Clear tokens, redirect to /login


LOGIN_FLOW:

1. User submits email + password
   │
   ▼
2. POST /api/auth/login
   │
   ├─► Success:
   │   - Store accessToken in localStorage
   │   - Store refreshToken in localStorage
   │   - Store user info in memory/store
   │   - Redirect to /dashboard
   │
   └─► Failure:
       - Show error message
       - Clear password field


LOGOUT_FLOW:

1. User clicks logout
   │
   ▼
2. POST /api/auth/logout (invalidate refresh token)
   │
   ▼
3. Clear localStorage tokens
   │
   ▼
4. Redirect to /login
```

### Auth Protection (Astro Middleware)

```
MIDDLEWARE_PSEUDOCODE:

# src/middleware.ts

FUNCTION onRequest(context, next):
    
    pathname = context.url.pathname
    
    # Public routes - no auth needed
    publicRoutes = ['/login', '/api/']
    IF publicRoutes.some(route => pathname.startsWith(route)):
        RETURN next()
    
    # Check for auth cookie/token
    token = context.cookies.get('accessToken')
    
    IF NOT token:
        RETURN context.redirect('/login')
    
    # Optionally validate token server-side
    # For now, client-side validation is sufficient
    
    RETURN next()
```

---

## State Management

Using **Nanostores** for lightweight reactive state:

```
STORES:

# src/stores/auth.ts

import { atom, computed } from 'nanostores'

# User store
$user = atom<User | null>(null)

$isAuthenticated = computed($user, user => user !== null)

$isAdmin = computed($user, user => user?.role === 'admin')

# Actions
setUser(user):
    $user.set(user)

clearUser():
    $user.set(null)


# Usage in React component:

import { useStore } from '@nanostores/react'
import { $user, $isAdmin } from '../stores/auth'

function Component():
    user = useStore($user)
    isAdmin = useStore($isAdmin)
    
    IF NOT user:
        RETURN <LoginPrompt />
    
    RETURN <div>Hello, {user.name}</div>
```

---

## Styling with Tailwind

### tailwind.config.js

```
TAILWIND_CONFIG:

content: ['./src/**/*.{astro,html,js,jsx,ts,tsx}']

theme:
  extend:
    colors:
      primary: '#3B82F6'      # Blue
      success: '#22C55E'      # Green
      warning: '#F59E0B'      # Yellow
      danger: '#EF4444'       # Red
      
      pothole:
        unverified: '#EAB308' # Yellow
        verified: '#22C55E'   # Green
        repaired: '#3B82F6'   # Blue
        false: '#6B7280'      # Gray

plugins:
  - @tailwindcss/forms
  - @tailwindcss/typography
```

### Component Style Patterns

```
STYLE_PATTERNS:

# Card
.card:
  @apply bg-white rounded-lg shadow-md p-6

# Button variants
.btn:
  @apply px-4 py-2 rounded-md font-medium transition-colors

.btn-primary:
  @apply btn bg-primary text-white hover:bg-primary/90

.btn-secondary:
  @apply btn bg-gray-100 text-gray-700 hover:bg-gray-200

.btn-danger:
  @apply btn bg-danger text-white hover:bg-danger/90

# Status badges
.badge:
  @apply px-2 py-1 rounded-full text-xs font-medium

.badge-unverified:
  @apply badge bg-yellow-100 text-yellow-800

.badge-verified:
  @apply badge bg-green-100 text-green-800

.badge-repaired:
  @apply badge bg-blue-100 text-blue-800

# Table
.table:
  @apply min-w-full divide-y divide-gray-200

.table-header:
  @apply px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase

.table-cell:
  @apply px-6 py-4 whitespace-nowrap text-sm text-gray-900
```

---

## Dependencies

```
DEPENDENCIES:

# Core
- astro
- @astrojs/react
- @astrojs/tailwind
- react
- react-dom

# UI Components
- @headlessui/react          # Accessible dropdowns, modals
- lucide-react               # Icons

# Map
- leaflet
- react-leaflet
- @react-leaflet/cluster     # Marker clustering

# Table
- @tanstack/react-table

# Forms
- react-hook-form
- @hookform/resolvers
- zod

# State
- nanostores
- @nanostores/react

# Utilities
- date-fns                   # Date formatting
- clsx                       # Conditional classes

# Charts (optional)
- chart.js
- react-chartjs-2
```

---

## Build & Development

### Development Server

```
# Start Astro dev server
npm run dev

# Runs on http://localhost:4321
# Hot reload enabled
# API proxy to backend (configure in astro.config.mjs)
```

### Production Build

```
# Build static site
npm run build

# Output in dist/
# - Static HTML pages
# - Hashed JS/CSS bundles
# - Optimized images

# Preview build locally
npm run preview
```

### Environment Variables

```
# .env
PUBLIC_API_URL=http://localhost:3000/api

# .env.production
PUBLIC_API_URL=https://api.yoursite.com
```

---

## Performance Optimizations

```
OPTIMIZATIONS:

1. Image Optimization
   - Use Astro's Image component
   - Lazy load pothole images
   - Serve WebP with JPEG fallback

2. Map Performance
   - Marker clustering for >100 markers
   - Lazy load map component
   - Debounce map move events

3. Table Performance
   - Virtual scrolling for large lists (if needed)
   - Pagination instead of infinite scroll
   - Debounce search input

4. Bundle Size
   - Astro islands = minimal JS
   - Dynamic imports for heavy components
   - Tree-shake unused Lucide icons

5. Caching
   - SWR for API calls (stale-while-revalidate)
   - Service worker for offline (optional)
```

---

## Testing Strategy

### Component Tests

```
COMPONENT_TESTS:

# Using Vitest + React Testing Library

- PotholeTable.test.tsx
  - renders_withData
  - sorts_byColumn
  - filters_byStatus
  - paginates_correctly

- StatusSelect.test.tsx
  - renders_currentStatus
  - calls_onChange_onSelect
  - shows_loading_state

- LoginForm.test.tsx
  - validates_emptyFields
  - validates_invalidEmail
  - submits_validData
  - shows_serverError
```

### E2E Tests (Optional)

```
E2E_TESTS:

# Using Playwright

- login.spec.ts
  - can_login_withValidCredentials
  - shows_error_withInvalidCredentials
  - redirects_toLogin_whenUnauthenticated

- potholes.spec.ts
  - can_viewPotholeList
  - can_filterByStatus
  - can_updateStatus
  - can_viewPotholeDetail
```

---

## Timeline

| Day | Tasks | Deliverables |
|-----|-------|--------------|
| Day 8 | Project setup, Astro + React + Tailwind | Empty project running |
| Day 9 | Layout components, sidebar, header | Dashboard shell |
| Day 10 | Login page, auth flow | Authentication working |
| Day 11 | Dashboard page, stats cards | Stats displaying |
| Day 12 | Pothole table component | List view working |
| Day 13 | Map component (Leaflet) | Map displaying markers |
| Day 14 | Pothole detail page | Detail view working |
| Day 15 | Status updates, actions | CRUD operations working |
| Day 16 | Vehicle management pages | Vehicle CRUD working |
| Day 17 | Polish, responsive design | Mobile-friendly |
| Day 18 | Testing, bug fixes | Stable dashboard |

**Total: ~11 days, overlapping with backend development**

---

## Deliverables

```
At the end of Week 3, Hamza should have:

□ Working dashboard
  └── Login/logout
  └── Dashboard overview with stats
  └── Pothole list with filters
  └── Interactive map view
  └── Pothole detail with status update
  └── Vehicle management

□ Source code
  └── Clean component structure
  └── TypeScript types
  └── API client

□ Build artifacts
  └── Production build in dist/
  └── Ready for deployment

□ Documentation
  └── Component props
  └── Environment setup
```

---

## Next Steps

After dashboard is complete:

1. **Integration testing** with backend API
2. **Deploy to Hetzner** (see [Deployment Guide](./05-deployment.md))
3. **Set up domain and SSL**
4. **User acceptance testing**

Continue to [Deployment Guide](./05-deployment.md)
