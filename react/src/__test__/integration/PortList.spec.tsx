import React from 'react';
import { render, fireEvent, waitFor, screen } from '@testing-library/react';
import PortList from "../../components/PortList";


describe('<PortList />', () => {
  it('contains ports from props with their appropriate links', () => {
    render(<PortList ports={['LHR', 'BHX']} drtDomain={'localhost'} />)
    expect(screen.getByText('LHR').closest('a')).toHaveAttribute('href', 'https://lhr.localhost');
    expect(screen.getByText('BHX').closest('a')).toHaveAttribute('href', 'https://bhx.localhost');
  });
});
