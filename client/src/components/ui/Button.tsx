import type { ButtonHTMLAttributes } from 'react'
import { Link, type LinkProps } from 'react-router-dom'
import { buttonStyles, type ButtonSize, type ButtonVariant } from '../../lib/buttonStyles'

type StyleProps = { variant?: ButtonVariant; size?: ButtonSize }

export function Button({
  variant,
  size,
  className,
  type = 'button',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & StyleProps) {
  return <button type={type} className={buttonStyles({ variant, size, className })} {...props} />
}

export function ButtonLink({ variant, size, className, ...props }: LinkProps & StyleProps) {
  return <Link className={buttonStyles({ variant, size, className })} {...props} />
}
