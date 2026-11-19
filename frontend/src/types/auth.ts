export interface RegisterResult {
  message?: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  status: number;
  errors?: Record<string, string>;

  constructor(
    status: number,
    errors?: Record<string, string>,
    message?: string
  ) {
    super(message ?? "API request failed");

    this.name = "ApiError";
    this.status = status;
    this.errors = errors;

    Object.setPrototypeOf(this, new.target.prototype);
  }
}
