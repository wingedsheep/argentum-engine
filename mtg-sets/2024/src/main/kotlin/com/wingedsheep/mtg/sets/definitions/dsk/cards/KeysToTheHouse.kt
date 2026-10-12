package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keys to the House — Duskmourn: House of Horror #251
 * {1}
 * Artifact
 *
 * {1}, {T}, Sacrifice this artifact: Search your library for a basic land card, reveal it, put it
 *   into your hand, then shuffle.
 * {3}, {T}, Sacrifice this artifact: Lock or unlock a door of target Room you control. Activate only
 *   as a sorcery.
 *
 * The second ability uses the resolution-time "lock or unlock a door" choice
 * ([Effects.LockOrUnlockDoor], a resolution-time choice of [Effects.LockDoor] / [Effects.UnlockDoor]):
 * the controller chooses lock or unlock as it resolves, then locks/unlocks one of the targeted
 * Room's doors (CR 709.5f/g) — prompting which door if the Room has more than one eligible door.
 * "Target Room you control" carries no locked/unlocked restriction: any Room you control always has
 * at least one door of one kind, so either choice can do something.
 */
val KeysToTheHouse = card("Keys to the House") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{3}, {T}, Sacrifice this artifact: Lock or unlock a door of target Room you control. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        )
        description = "{1}, {T}, Sacrifice this artifact: Search your library for a basic land card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        val room = target(TargetFilter(GameObjectFilter.Any.withSubtype(Subtype.ROOM).youControl()))
        effect = Effects.LockOrUnlockDoor(room)
        description = "{3}, {T}, Sacrifice this artifact: Lock or unlock a door of target Room you " +
            "control. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Artur Treffner"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c11a413-7f33-4b63-bdd9-e143e529f56d.jpg?1726286811"
    }
}
