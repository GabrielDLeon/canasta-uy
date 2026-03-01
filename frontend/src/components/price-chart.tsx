import { useEffect, useRef } from 'react'
import { ColorType, LineSeries, createChart } from 'lightweight-charts'

type ChartPoint = {
  time: string
  value: number
}

type PriceChartProps = {
  title: string
  points: ChartPoint[]
}

export function PriceChart({ title, points }: PriceChartProps) {
  const rootRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!rootRef.current || points.length === 0) {
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

    const line = chart.addSeries(LineSeries, {
      color: '#d97706',
      lineWidth: 2,
    })
    line.setData(points)
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
  }, [points])

  return (
    <section className="rounded-lg border bg-card p-4">
      <h3 className="mb-3 text-sm font-semibold text-muted-foreground">{title}</h3>
      {points.length === 0 ? (
        <p className="text-sm text-muted-foreground">Sin puntos para graficar.</p>
      ) : (
        <div ref={rootRef} className="w-full" />
      )}
    </section>
  )
}
