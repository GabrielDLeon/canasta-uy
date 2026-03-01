import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Item,
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
}

export function ProductItem({
  product,
  showImage = true,
  showActions = true,
}: ProductItemProps) {
  const productId = product.productId ?? product.id;

  return (
    <Link to={`/app/products/${productId}`} className="block">
      <Item variant="outline">
        {showImage && (
          <ItemMedia variant="image">
            <img
              src={`https://placehold.co/50x50`}
              width={32}
              height={32}
              className="object-cover grayscale rounded-xl"
              alt={product.name}
            />
          </ItemMedia>
        )}
        <ItemContent>
          <ItemTitle>{product.name}</ItemTitle>
          <ItemDescription>
            {product.brand ?? "Sin marca"}
          </ItemDescription>
        </ItemContent>
        {showActions && (
          <Button asChild size="sm" variant="secondary">
            <Link to={`/app/products/${productId}`}>Ver</Link>
          </Button>
        )}
      </Item>
    </Link>
  );
}
