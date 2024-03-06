import moment from "moment";
import { TerminalDataPoint } from '../../components/regionalpressure/regionalPressureSagas';

interface IStubService {
  generatePortPaxSeries: (start: string, end: string, interval: string, region: string, portCode: string[]) => TerminalDataPoint[]
}

class StubService implements IStubService {
  public generatePortPaxSeries(start: string, end: string, interval: string, region: string, portCodes: string[]) {
    const startDate = moment(start);
    const endDate = moment(end);
    const duration = moment.duration(endDate.diff(startDate));
    let momentUnit = 'hours';
    let durationInterval = 0;
    let randomRange = 0;
    switch (interval) {
      case 'daily':
        durationInterval = duration.asDays();
        randomRange = 2000
        momentUnit = 'days';
        break;
      case 'weekly':
        durationInterval = duration.asWeeks();
        randomRange = 10000
        momentUnit = 'weeks';
        break;
      default:
        durationInterval = 24;
        randomRange = 300
        momentUnit = 'hours';
        break;
    }
    const results: TerminalDataPoint[] = []
    portCodes.forEach((portCode) => { 
      for (let index = 0; index < durationInterval; index++) {
        const intervalDate = moment(startDate).add(index, momentUnit as moment.unitOfTime.DurationConstructor)
        const EEAPax = Math.floor(randomRange * Math.random())
        const eGatePax = Math.floor(randomRange * Math.random())
        const nonEEApax = Math.floor(randomRange * Math.random())
        results.push({
          date: intervalDate.startOf('day').format('YYYY-MM-DD'),
          hour: index,
          portCode: portCode,
          queueCounts: [
            {
              queueName: "EEA",
              count: EEAPax
            },
            {
              queueName: "e-Gates",
              count: eGatePax
            },
            {
              queueName: "Non-EEA",
              count: nonEEApax
            }
          ],
          regionName: region,
          totalPcpPax: EEAPax + eGatePax + nonEEApax,
        })
      }
    })
    return results
  }
}

const _ss = new StubService()
export default _ss;
