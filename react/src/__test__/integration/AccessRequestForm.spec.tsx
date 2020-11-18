import React from 'react';
import { rest } from 'msw'
import { setupServer } from 'msw/node'
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AccessRequestForm from "../../components/AccessRequestForm";

const server = setupServer(
  rest.post('/api/request-access', (req, res, ctx) => {
    return res(ctx.text('OK'))
  })
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('<AccessRequestForm />', () => {
  it('has a disabled submit button by default, and becomes enabled when a port is selected', () => {
    render(<AccessRequestForm ports={['LHR']} />);

    expect(screen.getByText('Request access').closest('button')).toHaveAttribute('disabled');

    fireEvent.click(screen.getByText('LHR'));

    expect(screen.getByText('Request access').closest('button')).not.toHaveAttribute('disabled');
  });

  it('has a disabled submit button when the last selected port becomes de-selected', () => {
    render(<AccessRequestForm ports={['LHR']} />);

    fireEvent.click(screen.getByText('LHR'));

    expect(screen.getByText('Request access').closest('button')).not.toHaveAttribute('disabled');

    fireEvent.click(screen.getByText('LHR'));

    expect(screen.getByText('Request access').closest('button')).toHaveAttribute('disabled');
  });

  it('displays a thank you message on submitting the form', async () => {
    render(<AccessRequestForm ports={['LHR']} />);

    fireEvent.click(screen.getByText('LHR'));

    expect(screen.getByText('Request access').closest('button')).not.toHaveAttribute('disabled');

    fireEvent.click(screen.getByText('Request access'));

    await waitFor(() => expect(screen.getByText('Thanks for your request. We\'ll get back to you shortly')));
  });
});
