export const adminMenuItems = [
  {label: 'Home', link: '/', roles: []},
  {label: 'Access requests', link: '/access-requests', roles: ['manage-users']},
  {label: 'Alert notices', link: '/alerts', roles: ['manage-users']},
  {label: 'Border-crossing import', link: '/border-crossing-import', roles: ['manage-users']},
  {label: 'Drop-in sessions', link: '/drop-in-sessions', roles: ['manage-users']},
  {label: 'Export config', link: '/export-config', roles: ['manage-users']},
  {label: 'Feature guides', link: '/feature-guides', roles: ['manage-users']},
  {label: 'Health checks', link: '/health-checks', roles: ['health-checks:edit']},
  {label: 'Health check pauses', link: '/health-check-pauses', roles: ['health-checks:edit']},
  {label: 'Feedback', link: '/user-feedback', roles: ['manage-users']},
  {label: 'Users', link: '/users', roles: ['manage-users']},
]
