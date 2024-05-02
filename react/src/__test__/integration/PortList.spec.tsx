import React from 'react';
import {render, act, screen} from '@testing-library/react';
import {PortList} from "../../components/PortList";


describe('<PortList />', () => {
  const user = {email: "", ports: [], roles:[]}
  it('contains ports from props with their appropriate links', () => {
    act(() => {
      render(<PortList userPorts={['LHR', 'BHX']} drtDomain={'localhost'} user={user} allRegions={[]} isRccUser={false}/>)
    });
    
    expect(screen.getByText('LHR').closest('a')).toHaveAttribute('href', 'https://lhr.localhost');
    expect(screen.getByText('BHX').closest('a')).toHaveAttribute('href', 'https://bhx.localhost');
  });
});
