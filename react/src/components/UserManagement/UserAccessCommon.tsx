import {GridColDef, GridValueGetterParams} from "@mui/x-data-grid";

function checkAllPorts(params: GridValueGetterParams) {
    return params.row.allPorts ? 'All ports' : params.row.portsRequested
}

export interface KeyCloakUser {
    id: string,
    username: string,
    enabled: boolean,
    emailVerified: boolean,
    firstName: string,
    lastName: string,
    email: string
}

export const columns: GridColDef[] = [
    {
        field: 'email',
        headerName: 'Email',
        width: 200
    },
    {
        field: 'requestTime',
        headerName: 'Request Time',
        width: 200,
    },
    {
        field: 'portsRequested',
        headerName: 'Ports',
        width: 150,
        valueGetter: checkAllPorts,
    },
    {
        field: 'regionsRequested',
        headerName: 'Regions',
        width: 100,
    },
    {
        field: 'staffEditing',
        headerName: 'Staffing',
        description: 'This column has a value getter and is not sortable.',
        sortable: false,
        width: 100,
    },
    {
        field: 'lineManager',
        headerName: 'Line Manager',
        width: 200,
    },

];