import React from 'react';
import axios from 'axios';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AccessRequestDetails, {UserRequestedAccessData} from '../../components/accessrequests/AccessRequestDetails';

jest.mock('axios');

type DeferredPromise<T> = {
  promise: Promise<T>;
  resolve: (value: T) => void;
};

const mockedAxios = axios as jest.Mocked<typeof axios>;

const baseAccessRequest: UserRequestedAccessData = {
  agreeDeclaration: true,
  allPorts: false,
  email: 'user1@test.com',
  lineManager: 'manager@test.com',
  portOrRegionText: 'Need airport access',
  portsRequested: 'LHR',
  accountType: 'standard',
  regionsRequested: '',
  requestTime: '2026-01-01T00:00:00.000Z',
  staffText: '',
  staffEditing: false,
  status: 'Requested',
};

const deferred = <T,>(): DeferredPromise<T> => {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((res) => {
    resolve = res;
  });

  return {promise, resolve};
};

const renderAccessRequestDetails = (props?: Partial<React.ComponentProps<typeof AccessRequestDetails>>) =>
  render(
    <AccessRequestDetails
      openModal={true}
      setOpenModal={jest.fn()}
      accessRequest={baseAccessRequest}
      status=""
      receivedUserDetails={true}
      setReceivedUserDetails={jest.fn()}
      {...props}
    />,
  );

describe('<AccessRequestDetails />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('waits for user-details GET and approval POST before showing success confirmation', async () => {
    const pendingApproval = deferred<{data: string}>();

    mockedAxios.get.mockResolvedValue({
      data: {
        id: 'kc-user-1',
        username: 'user1@test.com',
        enabled: true,
        emailVerified: true,
        firstName: 'User',
        lastName: 'One',
        email: 'user1@test.com',
      },
    } as any);
    mockedAxios.post.mockImplementation((url) => {
      if (url === '/api/users/accept-access-request/kc-user-1') {
        return pendingApproval.promise as any;
      }

      throw new Error(`Unexpected POST ${url}`);
    });

    renderAccessRequestDetails();

    fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(screen.queryByText('User access request approved')).not.toBeInTheDocument();

    pendingApproval.resolve({data: 'OK'});

    await waitFor(() => expect(screen.getByText('User access request approved')).toBeInTheDocument());
    expect(screen.getByText('The selected request was completed successfully.')).toBeInTheDocument();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
  });

  it('shows an approval failure confirmation when loading user details fails', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    mockedAxios.get.mockRejectedValue(new Error('Failed to load user details'));

    renderAccessRequestDetails();

    fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());
    expect(mockedAxios.post).not.toHaveBeenCalled();
    expect(screen.getByText('User access request could not be approved')).toBeInTheDocument();
    expect(screen.getByText('No selected requests were completed.')).toBeInTheDocument();
    expect(screen.getByText('Could not be approved')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
  });

  it('shows an approval failure confirmation when approval fails after loading user details', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    mockedAxios.get.mockResolvedValue({
      data: {
        id: 'kc-user-1',
        username: 'user1@test.com',
        enabled: true,
        emailVerified: true,
        firstName: 'User',
        lastName: 'One',
        email: 'user1@test.com',
      },
    } as any);
    mockedAxios.post.mockRejectedValue(new Error('Failed to approve user'));

    renderAccessRequestDetails();

    fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());
    expect(screen.getByText('User access request could not be approved')).toBeInTheDocument();
    expect(screen.getByText('No selected requests were completed.')).toBeInTheDocument();
    expect(screen.getByText('Could not be approved')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
  });

  it('waits for revert completion before showing revert confirmation', async () => {
    const pendingRevert = deferred<{data: string}>();

    mockedAxios.post.mockImplementation((url) => {
      if (url === '/api/users/update-access-request/Requested') {
        return pendingRevert.promise as any;
      }

      throw new Error(`Unexpected POST ${url}`);
    });

    renderAccessRequestDetails({status: 'Dismissed'});

    fireEvent.click(screen.getByRole('button', {name: 'Revert'}));

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(screen.queryByText('User access request reverted')).not.toBeInTheDocument();

    pendingRevert.resolve({data: 'OK'});

    await waitFor(() => expect(screen.getByText('User access request reverted')).toBeInTheDocument());
    expect(screen.getByText('The selected request was completed successfully.')).toBeInTheDocument();
    expect(screen.getByText('Reverted')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
  });

  it('shows a revert failure confirmation when the revert request fails', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    mockedAxios.post.mockRejectedValue(new Error('Failed to revert user'));

    renderAccessRequestDetails({status: 'Dismissed'});

    fireEvent.click(screen.getByRole('button', {name: 'Revert'}));

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalled());
    expect(screen.getByText('User access request could not be reverted')).toBeInTheDocument();
    expect(screen.getByText('No selected requests were completed.')).toBeInTheDocument();
    expect(screen.getByText('Could not be reverted')).toBeInTheDocument();
    expect(screen.getByText('user1@test.com')).toBeInTheDocument();
    expect(screen.getByText(/Please retry the failed users/)).toBeInTheDocument();
  });
});


