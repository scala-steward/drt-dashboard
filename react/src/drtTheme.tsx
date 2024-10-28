import {createTheme} from '@mui/material/styles';

const drtTheme = createTheme({
  palette: {
    primary: {
      main: '#005ea5',
    },
    secondary: {
      main: '#233E82'
    }
  },
  typography: {
    h1: {
      fontSize: '38px',
      fontWeight: 'bold',
      color: '#233E82'
    },
    h2: {
      fontSize: '32px',
      fontWeight: 'bold',
    },
    h3: {
      fontSize: "28px",
      fontWeight: "bold",
    },
    h4: {
      fontSize: "24px",
      fontWeight: "bold",
    },
    h5: {
      fontSize: "18px",
      fontWeight: "bold",
    },
    subtitle1: {
      fontSize: "19px",
      fontWeight: "bold",
    },
    subtitle2: {
      fontSize: "16px",
      fontWeight: "bold",
    },
    body1: {
      fontSize: "14px",
    },
    body2: {
      fontSize: "12px",
    },
    button: {
      fontSize: '18px',
      fontWeight: "bold",
      textTransform: 'none',
    },
  },

  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 400,
          fontSize: '16px',
          padding: '6px 12px',
        },
        outlined: {
          backgroundColor: '#fff'
        }
      },
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          margin: '0 !important'
        }
      }
    },
    MuiAccordionSummary: {
      styleOverrides: {
        root: {
          minHeight: 'unset !important',
        },
        content: {
          margin: '12px 0 0 !important'
        }
      }
    },
    MuiRadio:{
      styleOverrides:{
        root: {
          color: '#000',
          "&.Mui-checked": {
            color: '#000',
          }
        }
      }
    }
  },


});

export default drtTheme;
