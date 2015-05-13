package org.baobab.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TransactionView extends GridLayout {

    OnClickListener onAmountClick;
    TextView header;
    String account;
    double sum;
    boolean positive;
    boolean showImages = true;

    public TransactionView(Context context) {
        this(context, null);
    }

    public TransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColumnCount(6);
        setRowCount(2);
    }

    public void setOnAmountClick(OnClickListener onAmountClick) {
        this.onAmountClick = onAmountClick;
    }

    public void showImages(boolean showImages) {
        this.showImages = showImages;
    }

    public void populate(Cursor data) {
        removeAllViews();
        data.moveToPosition(-1);
        account = "";
        sum = 0.0;
        while (data.moveToNext()) {
            addProduct(data);
        }
    }

    private void addProduct(Cursor data) {
        float quantity = - data.getFloat(4);
        float price = data.getFloat(5);
        float total = quantity * price;
        sum += (quantity * price);
        if (!account.equals(data.getString(2)) || positive != quantity > 0) {
            account = data.getString(2);
            positive = quantity > 0;
            header = new TextView(getContext());
//            header.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(0, 6);
            lp.setGravity(Gravity.FILL_HORIZONTAL);
            FrameLayout f = new FrameLayout(getContext());
            if (onAmountClick != null) {
                if (account.equals("lager") || account.equals("kasse")) {
                    if (quantity > 0) {
                        header.setText("aus " + data.getString(11) + " raus");
                        header.setTextColor(getResources().getColor(R.color.medium_red));
                        f.setBackgroundResource(R.drawable.background_red);
                    } else {
                        header.setText("in " + data.getString(11) + " rein");
                        header.setTextColor(getResources().getColor(R.color.medium_green));
                        f.setBackgroundResource(R.drawable.background_green);
                    }
                } else  {
                    if (quantity > 0) {
                        header.setText("auf Konto gutschreiben");
                        header.setTextColor(getResources().getColor(R.color.medium_red));
                        f.setBackgroundResource(R.drawable.background_red);
                    } else {
                        header.setText("von Konto abbuchen");
                        header.setTextColor(getResources().getColor(R.color.medium_green));
                        f.setBackgroundResource(R.drawable.background_green);
                    }
                }
            }
            ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT );

            f.addView(header, p);
            addView(f, lp);
            f.setClickable(true);
            f.setId(data.getInt(3));
            f.setTag(String.valueOf(quantity));
            f.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues cv = new ContentValues();
                    cv.put("quantity", -1);
                    getContext().getContentResolver()
                            .update(((FragmentActivity) getContext())
                            .getIntent().getData(), cv, null, null);
                }
            });

        }
        LinearLayout images = new LinearLayout(getContext());
        if (!data.isNull(8) && showImages) {
            Uri img = Uri.parse(data.getString(8));
            images.setOrientation(LinearLayout.HORIZONTAL);
            int numberOfImages = account.equals("lager")? (int) Math.abs(quantity) : 1;
            for (int i = 0; i < numberOfImages; i++) {
                ImageView image = new ImageView(getContext());
                image.setImageURI(img);
                image.setPadding(2, 2, 2, 2);
                image.setScaleType(ImageView.ScaleType.FIT_XY);
                int width = getContext().getResources().getDimensionPixelSize(R.dimen.img_width);
                int height = getContext().getResources().getDimensionPixelSize(R.dimen.img_height);
                double factor = (2.0 - (1.0 / numberOfImages)) / numberOfImages;
                images.addView(image, new LinearLayout.LayoutParams((int) (width * factor ), height));
            }
            images.setClickable(true);
            final long product_id = data.getLong(3);
            images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getContext().getContentResolver().delete(
                            ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                    .appendEncodedPath("products/" + product_id)
                                    .build(), null, null);
                }
            });
        }
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_medium);
        addView(images, lp);

        TextView amount = new TextView(getContext());
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
        amount.setPadding(0, -padding, 0, -padding);
        if (data.getLong(3) <= 16) {
            amount.setText(String.valueOf((int) Math.abs(quantity)));
        } else {
            amount.setText("     ");
        }
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        amount.setBackgroundResource(R.drawable.background_translucent);
        amount.setTextColor(getResources().getColor(R.color.xlight_blue));
        FrameLayout f = new FrameLayout(getContext());
        f.addView(amount);
        addView(f, lp);
        f.setClickable(true);
        f.setId(data.getInt(3));
        f.setTag(String.valueOf(quantity));
        if (onAmountClick != null) {
            f.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAmountClick.onClick(v);
                }
            });
        }

        TextView x = new TextView(getContext());
        x.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        if (data.getLong(3) <= 16) {
            x.setText("x ");
        }
        x.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        lp.leftMargin = -2;
        addView(x, lp);

        TextView title = new TextView(getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        if (account.equals("lager") || account.equals("kasse")) {
            title.setText(data.getString(7));
        } else {
            title.setText(data.getString(12));
        }

        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 1, 0, -getContext().getResources().getDimensionPixelSize(R.dimen.padding_small));
        title.setTextColor(getResources().getColor(R.color.xlight_blue));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        addView(title, lp);

        TextView sum = new TextView(getContext());
        sum.setText(String.format("%.2f", Math.abs(total)));
        sum.setTypeface(null, Typeface.BOLD);
        if (quantity > 0) {
            sum.setTextColor(getResources().getColor(R.color.xdark_red));
        } else {
            sum.setTextColor(getResources().getColor(R.color.xdark_green));
        }
        sum.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
        f = new FrameLayout(getContext());
        f.addView(sum);
        f.setClickable(true);
        f.setId(data.getInt(3));
        f.setTag(String.valueOf(total));
        if (onAmountClick != null) {
            f.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAmountClick.onClick(v);
                }
            });
        }
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.bottomMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.setGravity(Gravity.RIGHT|Gravity.BOTTOM);
        addView(f, lp);

        TextView details = new TextView(getContext());
        details.setTextColor(getResources().getColor(android.R.color.black));
        details.setText(String.format("%.2f", price) + "/St");
        details.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xsmall));
        lp = new GridLayout.LayoutParams();
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        lp.columnSpec = GridLayout.spec(3);
        addView(details, lp);

        TextView eq = new TextView(getContext());
        eq.setText(" =");
        eq.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        eq.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.setGravity(Gravity.RIGHT);
        addView(eq, lp);

//        if (account == 4) { // Kasse
//            if (sum != 0.0) {
//                ContentValues b = new ContentValues();
//                b.put("quantity", - sum);
//                b.put("account_id", 4); // Kasse
//                getContext().getContentResolver().insert(
//                        ((FragmentActivity) getContext()).getIntent().getData()
//                                .buildUpon().appendEncodedPath("products/17").build(), b);
//            }
//        }
    }
}
