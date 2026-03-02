import { Link } from "react-router-dom";
import { Scale } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { useCompareProducts } from "@/hooks/use-compare-products";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemMedia,
  ItemTitle,
} from "@/components/ui/item";

interface ProductItemProps {
  product: {
    productId?: number;
    id?: number;
    name: string;
    brand?: string;
  };
  showImage?: boolean;
  showActions?: boolean;
  showCompareAction?: boolean;
}

export function ProductItem({
  product,
  showImage = true,
  showActions = true,
  showCompareAction = true,
}: ProductItemProps) {
  const productId = product.productId ?? product.id;
  const { addProduct } = useCompareProducts();

  const onAddToCompare = () => {
    const result = addProduct(product);

    if (result === "added") {
      toast.success("Producto agregado a comparacion.");
      return;
    }

    if (result === "duplicate") {
      toast("Este producto ya esta en comparacion.");
      return;
    }

    if (result === "limit") {
      toast.error("Solo puedes comparar hasta 5 productos.");
      return;
    }

    toast.error("No se pudo agregar el producto a comparacion.");
  };

  return (
    <Item
      variant="outline"
      className="border-dashed transition-all duration-150 hover:border-primary/50 hover:bg-accent/25 hover:shadow-sm"
    >
      {showImage && (
        <ItemMedia variant="image">
          <img
            src={`https://placehold.co/50x50`}
            width={32}
            height={32}
            className="rounded-xl object-cover grayscale"
            alt={product.name}
          />
        </ItemMedia>
      )}
      <ItemContent>
        {productId ? (
          <ItemTitle>
            <Link
              to={`/app/products/${productId}`}
              className="rounded-xs transition-colors hover:text-primary focus-visible:outline-none"
            >
              {product.name}
            </Link>
          </ItemTitle>
        ) : (
          <ItemTitle>{product.name}</ItemTitle>
        )}
        <ItemDescription>{product.brand ?? "Sin marca"}</ItemDescription>
      </ItemContent>
      {(showActions || showCompareAction) && (
        <ItemActions>
          {showCompareAction && productId ? (
            <Button
              type="button"
              size="sm"
              variant="outline"
              aria-label="Agregar a la comparacion"
              onClick={onAddToCompare}
            >
              <Scale className="size-4" />
            </Button>
          ) : null}
          {showActions && productId ? (
            <Button asChild size="sm" variant="secondary">
              <Link to={`/app/products/${productId}`}>Ver</Link>
            </Button>
          ) : null}
        </ItemActions>
      )}
    </Item>
  );
}
