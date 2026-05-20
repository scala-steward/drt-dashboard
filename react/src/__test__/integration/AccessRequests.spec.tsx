import React from 'react';
import axios from 'axios';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import AccessRequests from '../../components/accessrequests/AccessRequests';

jest.mock('axios');

jest.mock('@mui/x-data-grid', () => {
  const React = require('react');

  return {
    DataGrid: ({rows, getRowId, onRowSelectionModelChange}: any) => {
      const [selectedIds, setSelectedIds] = React.useState([] as (string | number)[]);

      return (
        <div>
          {rows.map((row: any) => {
            const id = getRowId ? getRowId(row) : row.id;
            const checked = selectedIds.includes(id);

            return (
              <label key={id}>
                <input
                  aria-label={`select-${row.email}`}
                  checked={checked}
                  type="checkbox"
                  onChange={(event) => {
                    const nextSelectedIds = event.target.checked
                      ? [...selectedIds, id]
                      : selectedIds.filter((selectedId: string | number) => selectedId !== id);

                    setSelectedIds(nextSelectedIds);
                    onRowSelectionModelChange?.(nextSelectedIds);
                  }}
                />
                {row.email}
              </label>
            );
          })}
        </div>
      );
    },
  };
});

type DeferredPromise<T> = {
  promise: Promise<T>;
  resolve: (value: T) => void;
};

const mockedAxios = axios as jest.Mocked<typeof axios>;

const buildAccessRequest = (email: string, requestTime: string) => ({
  agreeDeclaration: true,
  allPorts: false,
  email,
  lineManager: 'manager@test.com',
  portOrRegionText: 'Need airport access',
  portsRequested: 'LHR',
  accountType: 'standard',
  regionsRequested: '',
  requestTime,
  staffText: '',
  staffEditing: false,
  status: 'Requested',
});

const buildKeycloakUser = (email: string, index: number) => ({
  id: `kc-user-${index}`,
  username: email,
  enabled: true,
  emailVerified: true,
  firstName: `User${index}`,
  lastName: 'Test',
  email,
});

const buildBulkUsers = (count: number) => {
  const emails = Array.from({length: count}, (_, index) => `user${index + 1}@test.com`);
  const accessRequests = emails.map((email, index) => buildAccessRequest(email, `2026-01-01T00:${String(index).padStart(2, '0')}:00.000Z`));
  const keycloakUsers = Object.fromEntries(
    emails.map((email, index) => [email, buildKeycloakUser(email, index + 1)]),
  );

  return {emails, accessRequests, keycloakUsers};
};

const deferred = <T,>(): DeferredPromise<T> => {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((res) => {
    resolve = res;
  });

  return {promise, resolve};
};

describe('<AccessRequests /> bulk actions', () => {
  const accessRequests = [
    buildAccessRequest('user1@test.com', '2026-01-01T00:00:00.000Z'),
    buildAccessRequest('user2@test.com', '2026-01-01T00:01:00.000Z'),
  ];

  const keycloakUsers = {
    'user1@test.com': buildKeycloakUser('user1@test.com', 1),
    'user2@test.com': buildKeycloakUser('user2@test.com', 2),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = () => render(
    <MemoryRouter>
      <AccessRequests/>
    </MemoryRouter>,
  );

  const selectUsers = async (emails: string[]) => {
    for (const email of emails) {
      fireEvent.click(await screen.findByLabelText(`select-${email}`));
    }
  };

  const renderAndSelectUsers = async (emails: string[], buttonName: 'Approve' | 'Dismiss') => {
    renderComponent();
    await selectUsers(emails);
    fireEvent.click(screen.getByRole('button', {name: buttonName}));
  };

  it('waits for every selected approval to complete before showing the confirmation', async () => {
    const pendingSecondApproval = deferred<{data: string}>();

    mockedAxios.get.mockImplementation((url) => {
      switch (url) {
        case '/api/users/access-request?status=Requested':
          return Promise.resolve({data: accessRequests} as any);
        case '/api/users/user-details/user1@test.com':
          return Promise.resolve({data: keycloakUsers['user1@test.com']} as any);
        case '/api/users/user-details/user2@test.com':
          return Promise.resolve({data: keycloakUsers['user2@test.com']} as any);
        default:
          throw new Error(`Unexpected GET ${url}`);
      }
    });

    mockedAxios.post.mockImplementation((url) => {
      switch (url) {
        case '/api/users/accept-access-request/kc-user-1':
          return Promise.resolve({data: 'OK'} as any);
        case '/api/users/accept-access-request/kc-user-2':
          return pendingSecondApproval.promise as any;
        default:
          throw new Error(`Unexpected POST ${url}`);
      }
    });

    await renderAndSelectUsers(['user1@test.com', 'user2@test.com'], 'Approve');

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(2));
    expect(screen.queryByText('User access request approved')).not.toBeInTheDocument();

    pendingSecondApproval.resolve({data: 'OK'});

    await waitFor(() => expect(screen.getByText('User access request approved')).toBeInTheDocument());
    expect(screen.getByText('The selected requests were completed successfully.')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText('user2@test.com')).toBeInTheDocument();
  });

  it('shows a partial approval result with successful and failed users listed separately', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    mockedAxios.get.mockImplementation((url) => {
      switch (url) {
        case '/api/users/access-request?status=Requested':
          return Promise.resolve({data: accessRequests} as any);
        case '/api/users/user-details/user1@test.com':
          return Promise.resolve({data: keycloakUsers['user1@test.com']} as any);
        case '/api/users/user-details/user2@test.com':
          return Promise.resolve({data: keycloakUsers['user2@test.com']} as any);
        default:
          throw new Error(`Unexpected GET ${url}`);
      }
    });

    mockedAxios.post.mockImplementation((url) => {
      switch (url) {
        case '/api/users/accept-access-request/kc-user-1':
          return Promise.resolve({data: 'OK'} as any);
        case '/api/users/accept-access-request/kc-user-2':
          return Promise.reject(new Error('Keycloak group update failed'));
        default:
          throw new Error(`Unexpected POST ${url}`);
      }
    });

    await renderAndSelectUsers(['user1@test.com', 'user2@test.com'], 'Approve');

    await waitFor(() => expect(screen.getByText('User access request partially approved')).toBeInTheDocument());
    expect(screen.getByText('Some requests completed successfully, but some still need attention.')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.getByText(/user1@test\.com/)).toBeInTheDocument();
    expect(screen.getByText('Could not be approved')).toBeInTheDocument();
    expect(screen.getByText(/user2@test\.com/)).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  it('waits for the sixth selected approval to finish before showing bulk confirmation', async () => {
    const {emails, accessRequests, keycloakUsers} = buildBulkUsers(6);
    const pendingSixthApproval = deferred<{data: string}>();

    mockedAxios.get.mockImplementation((url) => {
      if (url === '/api/users/access-request?status=Requested') {
        return Promise.resolve({data: accessRequests} as any);
      }

      const email = url.replace('/api/users/user-details/', '');
      if (email in keycloakUsers) {
        return Promise.resolve({data: keycloakUsers[email as keyof typeof keycloakUsers]} as any);
      }

      throw new Error(`Unexpected GET ${url}`);
    });

    mockedAxios.post.mockImplementation((url) => {
      if (url === '/api/users/accept-access-request/kc-user-6') {
        return pendingSixthApproval.promise as any;
      }

      if (/^\/api\/users\/accept-access-request\/kc-user-[1-5]$/.test(url)) {
        return Promise.resolve({data: 'OK'} as any);
      }

      throw new Error(`Unexpected POST ${url}`);
    });

    await renderAndSelectUsers(emails, 'Approve');

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(6));
    expect(screen.queryByText('User access request approved')).not.toBeInTheDocument();

    pendingSixthApproval.resolve({data: 'OK'});

    await waitFor(() => expect(screen.getByText('User access request approved')).toBeInTheDocument());
    expect(screen.getByText('The selected requests were completed successfully.')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    emails.forEach(email => expect(screen.getByText(email)).toBeInTheDocument());
  });

  it('shows a partial approval result after a 6-user bulk approval with one failure', async () => {
    const {emails, accessRequests, keycloakUsers} = buildBulkUsers(6);
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    const failedEmail = emails[5];

    mockedAxios.get.mockImplementation((url) => {
      if (url === '/api/users/access-request?status=Requested') {
        return Promise.resolve({data: accessRequests} as any);
      }

      const email = url.replace('/api/users/user-details/', '');
      if (email in keycloakUsers) {
        return Promise.resolve({data: keycloakUsers[email as keyof typeof keycloakUsers]} as any);
      }

      throw new Error(`Unexpected GET ${url}`);
    });

    mockedAxios.post.mockImplementation((url) => {
      if (url === '/api/users/accept-access-request/kc-user-6') {
        return Promise.reject(new Error('Keycloak group update failed for user 6'));
      }

      if (/^\/api\/users\/accept-access-request\/kc-user-[1-5]$/.test(url)) {
        return Promise.resolve({data: 'OK'} as any);
      }

      throw new Error(`Unexpected POST ${url}`);
    });

    await renderAndSelectUsers(emails, 'Approve');

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(6));
    await waitFor(() => expect(screen.getByText('User access request partially approved')).toBeInTheDocument());
    expect(screen.getByText('Some requests completed successfully, but some still need attention.')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();

    emails.slice(0, 5).forEach(email => expect(screen.getByText(email)).toBeInTheDocument());
    expect(screen.getByText('Could not be approved')).toBeInTheDocument();
    expect(screen.getByText(failedEmail)).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  it('shows a full approval failure result when every selected approval fails', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    mockedAxios.get.mockImplementation((url) => {
      switch (url) {
        case '/api/users/access-request?status=Requested':
          return Promise.resolve({data: accessRequests} as any);
        case '/api/users/user-details/user1@test.com':
          return Promise.resolve({data: keycloakUsers['user1@test.com']} as any);
        case '/api/users/user-details/user2@test.com':
          return Promise.resolve({data: keycloakUsers['user2@test.com']} as any);
        default:
          throw new Error(`Unexpected GET ${url}`);
      }
    });

    mockedAxios.post.mockRejectedValue(new Error('Keycloak group update failed'));

    await renderAndSelectUsers(['user1@test.com', 'user2@test.com'], 'Approve');

    await waitFor(() => expect(screen.getByText('User access request could not be approved')).toBeInTheDocument());
    expect(screen.getByText('No selected requests were completed.')).toBeInTheDocument();
    expect(screen.getByText('Could not be approved')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText('user2@test.com')).toBeInTheDocument();
    expect(screen.queryByText('Approved')).not.toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  it('waits for the sixth selected dismissal to finish before showing bulk confirmation', async () => {
    const {emails, accessRequests} = buildBulkUsers(6);
    const pendingSixthDismissal = deferred<{data: string}>();

    mockedAxios.get.mockImplementation((url) => {
      if (url === '/api/users/access-request?status=Requested') {
        return Promise.resolve({data: accessRequests} as any);
      }

      throw new Error(`Unexpected GET ${url}`);
    });

    mockedAxios.post.mockImplementation((url) => {
      if (url !== '/api/users/update-access-request/Dismissed') {
        throw new Error(`Unexpected POST ${url}`);
      }

      const callIndex = mockedAxios.post.mock.calls.length;
      return callIndex === 6
        ? pendingSixthDismissal.promise as any
        : Promise.resolve({data: 'OK'} as any);
    });

    await renderAndSelectUsers(emails, 'Dismiss');

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(6));
    expect(screen.queryByText('User access request dismissed')).not.toBeInTheDocument();

    pendingSixthDismissal.resolve({data: 'OK'});

    await waitFor(() => expect(screen.getByText('User access request dismissed')).toBeInTheDocument());
    expect(screen.getByText('The selected requests were completed successfully.')).toBeInTheDocument();
    expect(screen.getByText('Dismissed')).toBeInTheDocument();
    emails.forEach(email => expect(screen.getByText(email)).toBeInTheDocument());
  });

  it('shows a partial dismissal result after a 6-user bulk dismissal with one failure', async () => {
    const {emails, accessRequests} = buildBulkUsers(6);
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    const failedEmail = emails[5];

    mockedAxios.get.mockImplementation((url) => {
      if (url === '/api/users/access-request?status=Requested') {
        return Promise.resolve({data: accessRequests} as any);
      }

      throw new Error(`Unexpected GET ${url}`);
    });

    mockedAxios.post.mockImplementation((url) => {
      if (url !== '/api/users/update-access-request/Dismissed') {
        throw new Error(`Unexpected POST ${url}`);
      }

      const callIndex = mockedAxios.post.mock.calls.length;
      return callIndex === 6
        ? Promise.reject(new Error('Failed to dismiss user 6'))
        : Promise.resolve({data: 'OK'} as any);
    });

    await renderAndSelectUsers(emails, 'Dismiss');

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(6));
    await waitFor(() => expect(screen.getByText('User access request partially dismissed')).toBeInTheDocument());
    expect(screen.getByText('Some requests completed successfully, but some still need attention.')).toBeInTheDocument();
    expect(screen.getByText('Dismissed')).toBeInTheDocument();

    emails.slice(0, 5).forEach(email => expect(screen.getByText(email)).toBeInTheDocument());
    expect(screen.getByText('Could not be dismissed')).toBeInTheDocument();
    expect(screen.getByText(failedEmail)).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  it('shows a full dismissal failure result when every selected dismissal fails', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);

    mockedAxios.get.mockImplementation((url) => {
      if (url === '/api/users/access-request?status=Requested') {
        return Promise.resolve({data: accessRequests} as any);
      }

      throw new Error(`Unexpected GET ${url}`);
    });

    mockedAxios.post.mockRejectedValue(new Error('Failed to dismiss users'));

    await renderAndSelectUsers(['user1@test.com', 'user2@test.com'], 'Dismiss');

    await waitFor(() => expect(screen.getByText('User access request could not be dismissed')).toBeInTheDocument());
    expect(screen.getByText('No selected requests were completed.')).toBeInTheDocument();
    expect(screen.getByText('Could not be dismissed')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText('user2@test.com')).toBeInTheDocument();
    expect(screen.queryByText('Dismissed')).not.toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });
});
