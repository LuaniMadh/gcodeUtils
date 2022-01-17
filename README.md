# gcodeUtils

This commandline-tool expexts gcodes in the format given by the repository https://github.com/sameer/svg2gcode.
It is used for plotting, for example with a 3D-printer which has a pen attached to it.

## set min/max X/Y

> --setMinX [the new smallest X-Value]
> --setMinY [the new smallest Y-Value]
> --setMaxX [the new biggest X-Value]
> --setMaxY [the new biggest Y-Value]

Shifts the .gcode automatically so that the smallest/biggest X/Y-Value is the one given here.

## setTool off/on
> --setToolOn [the new ToolOn command]
> --setToolOff [the new ToolOff command]

Replaces the toolOn/toolOff sequence. Note: the program only accepts a one-line-tool-on/off.

## regToolOff/On
> --regToolOff [current ToolOff command]
> --regToolOn [current ToolOn command]

If something went wrong automatically detecting the toolOn/toolOff sequence you can manually set it here. This may be needed for _--setToolOn/Off_ and _--optimize_.

## shiftX/Y

> --shiftX [distance mm]
> --shiftY [distance mm]

Moves every X/Y value in the gcode accordingly to the amount of millimeters given.

## file

> --file [path]

You will have to set a file to operate on *every* time you use the tool.

## optimize

> --optimize

Rearranges every path in a way that between every path there is the closest distance possible. Can improve paint/print time.
