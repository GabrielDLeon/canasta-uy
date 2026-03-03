import { useEffect, useMemo, useRef } from 'react'
import { AreaSeries, ColorType, createChart } from 'lightweight-charts'
import { useTheme } from 'next-themes'

type ChartPoint = {
  time: string
  value: number
}

type ChartSeries = {
  name: string
  color: string
  points: ChartPoint[]
}

type PriceChartProps = {
  title: string
  points?: ChartPoint[]
  series?: ChartSeries[]
}

function hexToRgba(color: string, alpha: number): string {
  const hex = color.replace('#', '')

  if (hex.length !== 6) {
    return color
  }

  const normalized = Number.parseInt(hex, 16)
  const red = (normalized >> 16) & 255
  const green = (normalized >> 8) & 255
  const blue = normalized & 255

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

export function PriceChart({ title, points = [], series }: PriceChartProps) {
  const { resolvedTheme } = useTheme()
  const rootRef = useRef<HTMLDivElement | null>(null)
  const resolvedSeries = useMemo(
    () => (series && series.length > 0 ? series : [{ name: title, color: '#9c5a24', points }]),
    [points, series, title],
  )
  const hasData = resolvedSeries.some((item) => item.points.length > 0)

  useEffect(() => {
    if (!rootRef.current || !hasData) {
      return
    }

    const rootStyles = getComputedStyle(document.documentElement)
    const getVar = (name: string, fallback: string) => {
      const value = rootStyles.getPropertyValue(name).trim()
      return value || fallback
    }

    const chartBackground = getVar('--card', resolvedTheme === 'dark' ? '#2a2118' : '#fffaf0')
    const chartText = getVar('--muted-foreground', resolvedTheme === 'dark' ? '#c5b08c' : '#6d5b45')
    const chartBorder = getVar('--border', resolvedTheme === 'dark' ? '#4d3a2a' : '#d9c9a5')
    const chartGrid = getVar('--muted', resolvedTheme === 'dark' ? '#2f241a' : '#f3e9d3')

    const chart = createChart(rootRef.current, {
      height: 300,
      layout: {
        background: { type: ColorType.Solid, color: chartBackground },
        textColor: chartText,
      },
      rightPriceScale: {
        borderColor: chartBorder,
      },
      timeScale: {
        borderColor: chartBorder,
      },
      grid: {
        vertLines: { color: chartGrid },
        horzLines: { color: chartGrid },
      },
    })

    resolvedSeries.forEach((item) => {
      if (item.points.length === 0) {
        return
      }

      const topColor = hexToRgba(item.color, resolvedTheme === 'dark' ? 0.36 : 0.28)
      const bottomColor = hexToRgba(item.color, resolvedTheme === 'dark' ? 0.06 : 0.04)

      const line = chart.addSeries(AreaSeries, {
        lineColor: item.color,
        topColor,
        bottomColor,
        lineWidth: 2,
      })
      line.setData(item.points)
    })

    chart.timeScale().fitContent()

    const observer = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (!entry) {
        return
      }

      chart.applyOptions({ width: entry.contentRect.width })
    })

    observer.observe(rootRef.current)

    return () => {
      observer.disconnect()
      chart.remove()
    }
  }, [hasData, resolvedSeries, resolvedTheme])

  return (
    <section className="rounded-lg border bg-card p-4">
      <h3 className="mb-3 text-sm font-semibold text-muted-foreground">{title}</h3>
      {resolvedSeries.length > 1 ? (
        <div className="mb-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
          {resolvedSeries.map((item) => (
            <div key={item.name} className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full" style={{ backgroundColor: item.color }} />
              <span>{item.name}</span>
            </div>
          ))}
        </div>
      ) : null}
      {!hasData ? (
        <p className="text-sm text-muted-foreground">Sin puntos para graficar.</p>
      ) : (
        <div ref={rootRef} className="w-full" />
      )}
    </section>
  )
}
