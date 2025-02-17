import moment from "moment/moment";
import {getHistoricDateByDay} from "../../components/regionalpressure/regionalPressureState";

describe('Regional Pressure Sagas: Calculates the same day the of the previous year', () => {

  it('Leap year: Monday April 1st 2024 -> Monday April 3rd 2023', async () => {
    var historicDate = getHistoricDateByDay(moment('2024-04-01'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Monday April 3rd 2023');
  });

  it('Leap year: Thursday February 29th 2024 -> Thursday March 2nd 2023', async () => {
    var historicDate = getHistoricDateByDay(moment('2024-02-29'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Thursday March 2nd 2023');
  });

  it('Leap year: Friday March 1st 2024 -> Friday March 3rd 2023', async () => {
    var historicDate = getHistoricDateByDay(moment('2024-03-01'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Friday March 3rd 2023');
  });

  it('Wednesday Jan 1st 2025 -> Wednesday January 3rd 2024', async () => {
    var historicDate = getHistoricDateByDay(moment('2025-01-01'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Wednesday January 3rd 2024');
  });

  it('Wednesday July 22nd 2026 -> Wednesday July 23rd 2025', async () => {
    var historicDate = getHistoricDateByDay(moment('2026-07-22'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Wednesday July 23rd 2025');
  });

  it('Tuesday December 1st 2026 -> Tuesday December 2nd 2025', async () => {
    var historicDate = getHistoricDateByDay(moment('2026-12-01'))
    expect(historicDate.format('dddd MMMM Do YYYY')).toBe('Tuesday December 2nd 2025');
  });


})
