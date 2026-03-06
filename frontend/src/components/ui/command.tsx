"use client"

import * as React from "react"
import { Command as CommandPrimitive } from "cmdk"
import { SearchIcon } from "lucide-react"

import { cn } from "@/lib/utils"
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog"

function Command({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive>) {
  return (
    <CommandPrimitive
      data-slot="command"
      className={cn("bg-popover text-popover-foreground flex h-full w-full flex-col overflow-hidden", className)}
      {...props}
    />
  )
}

function CommandDialog({
  title = "Command Palette",
  description = "Search for a command to run...",
  children,
  className,
  showCloseButton = false,
  ...props
}: React.ComponentProps<typeof Dialog> & {
  title?: string
  description?: string
  className?: string
  showCloseButton?: boolean
}) {
  return (
    <Dialog {...props}>
      <DialogContent className="overflow-hidden p-0" showCloseButton={showCloseButton}>
        <DialogTitle className="sr-only">{title}</DialogTitle>
        <DialogDescription className="sr-only">{description}</DialogDescription>
        <Command
          className={cn(
            "[&_[cmdk-group-heading]]:text-muted-foreground [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1.5 [&_[cmdk-group-heading]]:text-xs [&_[cmdk-group]:not([hidden])_~[cmdk-group]]:pt-0 [&_[cmdk-group]]:px-1 [&_[cmdk-group]]:py-0 [&_[cmdk-input-wrapper]_svg]:size-4 [&_[cmdk-input]]:h-11 [&_[cmdk-input]]:w-full [&_[cmdk-input]]:bg-transparent [&_[cmdk-input]]:px-2 [&_[cmdk-input]]:py-3 [&_[cmdk-input]]:text-sm [&_[cmdk-input]]:outline-none [&_[cmdk-item]]:data-[selected=true]:bg-accent [&_[cmdk-item]]:data-[selected=true]:text-accent-foreground [&_[cmdk-item]]:relative [&_[cmdk-item]]:flex [&_[cmdk-item]]:cursor-default [&_[cmdk-item]]:items-center [&_[cmdk-item]]:gap-2 [&_[cmdk-item]]:rounded-sm [&_[cmdk-item]]:px-2 [&_[cmdk-item]]:py-1.5 [&_[cmdk-item]]:text-sm [&_[cmdk-item]]:outline-none [&_[cmdk-item]]:select-none [&_[cmdk-item]]:data-[disabled=true]:pointer-events-none [&_[cmdk-item]]:data-[disabled=true]:opacity-50 [&_[cmdk-list]]:max-h-[320px] [&_[cmdk-list]]:overflow-y-auto [&_[cmdk-separator]]:bg-border [&_[cmdk-separator]]:-mx-1 [&_[cmdk-separator]]:h-px [&_[cmdk-separator]]:my-1",
            className
          )}
        >
          {children}
        </Command>
      </DialogContent>
    </Dialog>
  )
}

function CommandInput({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive.Input>) {
  return (
    <div
      data-slot="command-input-wrapper"
      className="flex items-center gap-2 border-b px-2.5"
      cmdk-input-wrapper=""
    >
      <SearchIcon className="text-muted-foreground size-4 shrink-0" />
      <CommandPrimitive.Input
        data-slot="command-input"
        className={cn(
          "placeholder:text-muted-foreground flex h-10 w-full rounded-md bg-transparent py-3 text-sm outline-none disabled:cursor-not-allowed disabled:opacity-50",
          className
        )}
        {...props}
      />
    </div>
  )
}

function CommandList({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive.List>) {
  return (
    <CommandPrimitive.List
      data-slot="command-list"
      className={cn("max-h-[300px] overflow-y-auto overflow-x-hidden p-1", className)}
      {...props}
    />
  )
}

function CommandEmpty(props: React.ComponentProps<typeof CommandPrimitive.Empty>) {
  return <CommandPrimitive.Empty data-slot="command-empty" className="py-6 text-center text-sm" {...props} />
}

function CommandGroup({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive.Group>) {
  return (
    <CommandPrimitive.Group
      data-slot="command-group"
      className={cn(
        "text-foreground [&_[cmdk-group-heading]]:text-muted-foreground [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1.5 [&_[cmdk-group-heading]]:text-xs [&_[cmdk-group-heading]]:font-medium",
        className
      )}
      {...props}
    />
  )
}

function CommandSeparator({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive.Separator>) {
  return <CommandPrimitive.Separator data-slot="command-separator" className={cn("bg-border -mx-1 h-px", className)} {...props} />
}

function CommandItem({
  className,
  ...props
}: React.ComponentProps<typeof CommandPrimitive.Item>) {
  return (
    <CommandPrimitive.Item
      data-slot="command-item"
      className={cn(
        "data-[selected=true]:bg-accent data-[selected=true]:text-accent-foreground relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none select-none data-[disabled=true]:pointer-events-none data-[disabled=true]:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
        className
      )}
      {...props}
    />
  )
}

function CommandShortcut({ className, ...props }: React.ComponentProps<"span">) {
  return (
    <span
      data-slot="command-shortcut"
      className={cn("text-muted-foreground ml-auto text-xs tracking-widest", className)}
      {...props}
    />
  )
}

export {
  Command,
  CommandDialog,
  CommandInput,
  CommandList,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandShortcut,
  CommandSeparator,
}
