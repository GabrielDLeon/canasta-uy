import { useEffect, useRef } from 'react'
import { ColorType, LineSeries, createChart } from 'lightweight-charts'

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

export function PriceChart({ title, points = [], series }: PriceChartProps) {
  const rootRef = useRef<HTMLDivElement | null>(null)
  const resolvedSeries =
    series && series.length > 0 ? series : [{ name: title, color: '#d97706', points }]
  const hasData = resolvedSeries.some((item) => item.points.length > 0)

  useEffect(() => {
    if (!rootRef.current || !hasData) {
      return
    }

    const chart = createChart(rootRef.current, {
      height: 300,
      layout: {
        background: { type: ColorType.Solid, color: 'white' },
        textColor: '#555',
      },
      rightPriceScale: {
        borderColor: '#ddd',
      },
      timeScale: {
        borderColor: '#ddd',
      },
      grid: {
        vertLines: { color: '#f1f1f1' },
        horzLines: { color: '#f1f1f1' },
      },
    })

    resolvedSeries.forEach((item) => {
      if (item.points.length === 0) {
        return
      }

      const line = chart.addSeries(LineSeries, {
        color: item.color,
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
  }, [hasData, resolvedSeries])

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
