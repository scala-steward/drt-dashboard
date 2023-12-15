import moment from "moment";

export interface FormError {
  field: string,
  message: string,
}

interface FormFields {
  [key: string]: any
}

interface FieldValidations {
  name: string;
  validator: (value: any, otherFields?: FormFields) => boolean;
  message: string;
}

export interface FormValidations {
  [key: string]: FieldValidations[]
}

interface IValidationService {
  common: object;
  validateForm: (validators: FormValidations, values: FormFields) => object[];
}


class ValidationService implements IValidationService {

  public common = {
    'isDate': (value: string) => {
      return moment(value).isValid()
    },
    'required': (value: string) => {
      return !!value
    },
  }

  public validateForm(validators: FormValidations, values: FormFields) {
    let formErrors :FormError[] = []
    Object.keys(values).forEach((fieldName: string) => {
      const fieldErrors :FormError[] = [];
      const fieldValidators = validators[fieldName];
      const value = values[fieldName];
      fieldValidators.forEach((field) => {
        let passing = field.validator(value, values);
        if (!passing) {
          fieldErrors.push({
            field: fieldName,
            message: field.message,
          })
        }
      })
      formErrors = [
        ...formErrors,
        ...fieldErrors
      ]
    })

    return formErrors
  }
}

const vs = new ValidationService();
export default vs;
