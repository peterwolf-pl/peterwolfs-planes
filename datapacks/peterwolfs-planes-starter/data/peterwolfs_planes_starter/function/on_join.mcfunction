# First-join only: advancement stays completed (do not revoke) so this runs once.
execute if entity @s[tag=peterwolfs_planes_starter_received] run return 0
function peterwolfs_planes_starter:give
tag @s add peterwolfs_planes_starter_received
