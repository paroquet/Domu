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
      className={cn("p-4 bg-white", className)}
      classNames={{
        months: "flex flex-col sm:flex-row gap-4",
        month: "flex flex-col gap-4 relative",
        month_caption: "flex justify-center items-center h-9",
        caption_label: "text-sm font-medium text-gray-900",
        nav: "absolute top-0 left-0 right-0 flex items-center justify-between h-9 px-1",
        button_previous: cn(
          "h-8 w-8 flex items-center justify-center rounded-lg",
          "text-gray-500 hover:text-gray-900 hover:bg-gray-100 transition-colors"
        ),
        button_next: cn(
          "h-8 w-8 flex items-center justify-center rounded-lg",
          "text-gray-500 hover:text-gray-900 hover:bg-gray-100 transition-colors"
        ),
        month_grid: "w-full border-collapse",
        weekdays: "flex",
        weekday: "text-gray-500 rounded-md w-9 font-normal text-[0.8rem] text-center",
        week: "flex w-full mt-2",
        day: "h-9 w-9 text-center text-sm p-0 relative",
        day_button: cn(
          "h-9 w-9 p-0 font-normal rounded-md transition-colors",
          "hover:bg-gray-100 hover:text-gray-900",
          "focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1"
        ),
        range_end: "day-range-end",
        selected:
          "bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 focus:bg-blue-700",
        today: "bg-blue-100 text-blue-600 font-semibold rounded-md",
        outside:
          "day-outside text-gray-300 aria-selected:bg-blue-100 aria-selected:text-blue-400",
        disabled: "text-gray-300",
        range_middle:
          "aria-selected:bg-blue-50 aria-selected:text-blue-600",
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
