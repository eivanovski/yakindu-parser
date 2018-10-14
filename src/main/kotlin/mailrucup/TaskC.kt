package mailrucup

import java.util.*
/*


private fun readLineSure() = (readLine() ?: throw RuntimeException())

private fun readInts() = readLineSure().trim().split("\\s+".toRegex()).map { it.toInt() }

enum class Color {
    Red, Green, Blue
}

fun Char.convert() = when (this) {
    'R' -> Color.Red
    'G' -> Color.Green
    'B' -> Color.Blue
    else -> throw RuntimeException()
}

data class Box(val pos: Int, val candies: Int, val color: Color)
class Path(
        val prev: Path,

)


fun main(args: Array<String>) {
    val (boxCount, startPos, target) = readInts()
    val candiesInBox = readInts().toTypedArray()
    val colors = readLineSure()

    val array = Array<TreeSet<Box>>(Color.values().size) { TreeSet(Comparator.comparingInt(Box::candies)) }
    for (i in 1..boxCount) {
        val box = Box(i, candiesInBox[i - 1], colors[i - 1].convert())
        array[box.color.ordinal].add(box)
    }


}
*/
