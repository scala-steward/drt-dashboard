import * as React from 'react';
import {connect, MapDispatchToProps} from 'react-redux';
import {
  Box,
  Button,
  Checkbox,
  FormControl,
  FormControlLabel,
  Radio,
  RadioGroup,
  SelectChangeEvent
} from "@mui/material";

import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import moment, {Moment} from 'moment';
import DownloadPorts from './DownloadPorts';
import DownloadModal from './DownloadModal';

import {checkDownloadStatus, PortTerminal, requestDownload} from './downloadManagerSagas';
import {RootState} from '../../store/redux';
import {UserProfile} from "../../model/User";
import {ConfigValues, PortRegion} from "../../model/Config";
import {create} from 'lodash';

interface DownloadDates {
  start: Moment;
  end: Moment;
}

interface DownloadManagerProps {
  status: string;
  createdAt: string;
  downloadUrl: string;
  user: UserProfile;
  config: ConfigValues;
  requestDownload: (ports: PortTerminal[], breakdown: string, startDate: Moment, endDate: Moment) => void;
  checkDownloadStatus: (createdAt: string) => void;
}


const DownloadManager = ({status, createdAt, downloadUrl, requestDownload, user, config, checkDownloadStatus}: DownloadManagerProps) => {
  const [modalOpen, setModalOpen] = React.useState<boolean>(false);
  const [selectedPorts, setSelectedPorts] = React.useState<string[]>([]);
  const [dates, setDate] = React.useState<DownloadDates>({
    start: moment().subtract(1, 'day'),
    end: moment(),
  });
  const [breakdown, setBreakdown] = React.useState<string>('passengers-port');
  const [daily, setDaily] = React.useState<boolean>(false);

  const isRccRegion = (regionName : string) => {
    return user.roles.includes("rcc:" + regionName.toLowerCase())
  }

  let interval: {current: ReturnType<typeof setInterval> | null | any} = React.useRef(null);

  React.useEffect(() => {
    console.log(create, status);
    if (createdAt && status === 'preparing') {
      interval.current = setInterval(()=>{
        checkDownloadStatus(createdAt);
      }, 2000);
    }
    return () => {
      clearInterval(interval.current);
    };
  }, [createdAt, status])

  const userPortsByRegion: PortRegion[] = config.portsByRegion.map(region => {
    const userPorts: string[] = user.ports.filter(p => region.ports.includes(p));
    return {...region, ports: userPorts} as PortRegion
  }).filter(r => r.ports.length > 0 || isRccRegion(r.name))

  const onDateChange = (type: string, date: Moment | null) => {
    setDate({
      ...dates,
      [type]: date
    });
  }

  const handleModalClose = () => {
    setModalOpen(false)
    clearInterval(interval.current);
  }

  const onBreakdownChange = (event: React.ChangeEvent<HTMLInputElement>, breakdown: string) => {
    setBreakdown(breakdown);
  }

  const handlePortChange = (event: SelectChangeEvent<typeof selectedPorts>) => {
    const {
      target: { value },
    } = event;
    setSelectedPorts(typeof value === 'string' ? value.split(',') : value);
  };

  const handlePortCheckboxChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {
      target: { name },
    } = event;
    setSelectedPorts(selectedPorts.includes(name) ? selectedPorts.filter(port => port !== name) : [...selectedPorts, name]);
  }

  const handlePortCheckboxGroupChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {
      target: { name, checked },
    } = event;
    const region = userPortsByRegion.filter(region => region.name === name)[0]
    if (checked) {
      // add all in region
      const newSelection = [...selectedPorts, ...region.ports]
      const deduped = newSelection.filter((element, index) => {
        return newSelection.indexOf(element) === index;
      });
      setSelectedPorts(deduped)
    } else {
      // remove any selected from region
      setSelectedPorts(selectedPorts.filter(port => !region.ports.includes(port)))
    }
  }

  const disablePassengerBreakdown = (): boolean =>  {
    return (breakdown == 'flight') || (Math.abs(dates.start.diff(dates.end, 'days')) < 1)
  }

  const handleSubmit = () :void => {

    const portsWithTerminals :PortTerminal[] = config.ports.filter((port) => selectedPorts.includes(port.iata)).map(port => {
      return {
        port: port.iata,
        terminals: port.terminals
      }
    });
    requestDownload(portsWithTerminals, breakdown, dates.start, dates.end);
    setModalOpen(true);
  }

  const canRequestReport = () :boolean => {
    return (config.ports.filter((port) => selectedPorts.includes(port.iata)).length <= 0)
  }

  return (
    <Box>
      <h1>Download Manager</h1>
      <Box sx={{backgroundColor: '#E6E9F1', p: 2}}>
        <Box>
          <h3>Date Range</h3>
          <DatePicker label="Start" sx={{backgroundColor: '#fff', marginRight: '10px'}}
                                      value={dates.start}
                                      maxDate={dates.end}
                                      onChange={(newValue: Moment | null) => onDateChange('start', newValue)}/>
          <DatePicker label="End"  sx={{backgroundColor: '#fff'}}
                                      value={dates.end}
                                      onChange={(newValue: Moment | null) => onDateChange('end', newValue)}/>
        </Box>

        <DownloadPorts
          handlePortChange={handlePortChange}
          handlePortCheckboxChange={handlePortCheckboxChange}
          handlePortCheckboxGroupChange={handlePortCheckboxGroupChange}
          portsByRegion={userPortsByRegion}
          selectedPorts={selectedPorts}
          />

        <h3>Passenger totals breakdown</h3>
        <Box sx={{padding: '1em', backgroundColor: '#fff'}}>
          <FormControl sx={{width: '100%', paddingBottom: '1em'}}>
            <RadioGroup
              aria-labelledby="demo-radio-buttons-group-label"
              defaultValue="female"
              name="radio-buttons-group"
              onChange={onBreakdownChange}
              value={breakdown}
            >
              <FormControlLabel value="passengers-port" control={<Radio />} label="By port" />
              <FormControlLabel value="passengers-terminal" control={<Radio />} label="By terminal" />
              <FormControlLabel value="arrrivals" control={<Radio />} label="By flight" />
            </RadioGroup>
          </FormControl>
          <FormControl>
            <FormControlLabel control={<Checkbox disabled={disablePassengerBreakdown()} value={daily} onChange={()=> setDaily(!daily)} inputProps={{ 'aria-label': 'controlled' }} />} label={'Daily passenger breakdown'} />
          </FormControl>
        </Box>
        <Box>
        </Box>


        <Box sx={{marginTop: '1em'}}>
          <Button disabled={canRequestReport()} onClick={() => handleSubmit()} color='primary' variant='contained'>Create Report</Button>
        </Box>

        <DownloadModal status={status} downloadUrl={downloadUrl} isModalOpen={modalOpen} handleModalClose={handleModalClose} />
      </Box>
    </Box>
  )
}

const mapState = (state: RootState) => {
  return {
    status: state.downloadManager?.status,
    createdAt: state.downloadManager?.createdAt,
    downloadUrl: state.downloadManager?.downloadLink,
  };
}

const mapDispatch = (dispatch :MapDispatchToProps<any, DownloadManagerProps>) => {
  return {
    checkDownloadStatus: (createdAt: string) => {
      dispatch(checkDownloadStatus(createdAt));
    },
    requestDownload: (ports: PortTerminal[], breakdown: string, startDate: Moment, endDate: Moment) => {
      dispatch(requestDownload(ports, breakdown, startDate.toISOString(), endDate.toISOString()))
    },
  };
};

export default connect(mapState, mapDispatch)(DownloadManager);
