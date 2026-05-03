import * as React from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { DayPicker } from "react-day-picker"

import { cn } from "@/lib/utils"

export type CalendarProps = React.ComponentProps<typeof DayPicker>

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  ...props
}: CalendarProps) {
  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn("p-4 bg-background text-foreground", className)}
      classNames={{
        months: "flex flex-col sm:flex-row gap-4",
        month: "flex flex-col gap-4 relative",
        month_caption: "flex justify-center items-center h-9",
        caption_label: "text-sm font-medium text-foreground",
        nav: "absolute top-0 left-0 right-0 flex items-center justify-between h-9 px-1",
        button_previous: cn(
          "h-8 w-8 flex items-center justify-center rounded-lg",
          "text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
        ),
        button_next: cn(
          "h-8 w-8 flex items-center justify-center rounded-lg",
          "text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
        ),
        month_grid: "w-full border-collapse",
        weekdays: "flex",
        weekday: "text-muted-foreground rounded-md w-9 font-normal text-[0.8rem] text-center",
        week: "flex w-full mt-2",
        day: "h-9 w-9 text-center text-sm p-0 relative",
        day_button: cn(
          "h-9 w-9 p-0 font-normal rounded-md transition-colors",
          "hover:bg-accent hover:text-accent-foreground",
          "focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1"
        ),
        range_end: "day-range-end",
        selected:
          "bg-primary text-primary-foreground font-medium rounded-md hover:bg-primary/90 focus:bg-primary/90",
        today: "bg-primary/10 text-primary font-semibold rounded-md",
        outside:
          "day-outside text-muted-foreground/50 aria-selected:bg-primary/10 aria-selected:text-primary/60",
        disabled: "text-muted-foreground/50",
        range_middle:
          "aria-selected:bg-primary/10 aria-selected:text-primary",
        hidden: "invisible",
        ...classNames,
      }}
      components={{
        Chevron: ({ orientation }) => {
          const Icon = orientation === "left" ? ChevronLeft : ChevronRight
          return <Icon className="h-4 w-4" />
        },
      }}
      {...props}
    />
  )
}
Calendar.displayName = "Calendar"

export { Calendar }
