import { type ButtonHTMLAttributes, type ReactNode } from 'react'
import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Visual style variant */
  variant?: ButtonVariant
  /** Size of the button */
  size?: ButtonSize
  /** Whether the button takes full width of container */
  fullWidth?: boolean
  /** Whether the button is in active/selected state */
  active?: boolean
  /** Button content */
  children: ReactNode
}

/**
 * Reusable button component with consistent styling.
 * Uses CSS modules for hover states instead of JS event handlers.
 */
export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  active = false,
  className,
  children,
  ...props
}: ButtonProps) {
  const classNames = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth && styles.fullWidth,
    active && styles.active,
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button className={classNames} {...props}>
      {children}
    </button>
  )
}
