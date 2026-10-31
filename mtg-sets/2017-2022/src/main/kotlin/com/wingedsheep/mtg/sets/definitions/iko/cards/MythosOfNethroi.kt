package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mythos of Nethroi — Ikoria: Lair of Behemoths #97
 * {2}{B} · Instant
 *
 * Destroy target nonland permanent if it's a creature or if {G}{W} was spent to cast this spell.
 *
 * Any nonland permanent is a legal target; both halves of the "or" are checked as the spell
 * resolves (per the rulings): whether the target is a creature *then* (projected state, so an
 * animated land-less permanent or a crewed Vehicle counts), or whether at least one {G} and one
 * {W} were actually paid. A copy had no mana spent on it, so it only ever destroys creatures.
 */
val MythosOfNethroi = card("Mythos of Nethroi") {
    manaCost = "{2}{B}"
    colorIdentity = "BGW"
    typeLine = "Instant"
    oracleText = "Destroy target nonland permanent if it's a creature or if {G}{W} was spent to cast this spell."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.If(
            condition = Conditions.Any(
                Conditions.TargetMatchesFilter(GameObjectFilter.Creature, permanent),
                Conditions.ManaSpentToCastIncludes(requiredWhite = 1, requiredGreen = 1),
            ),
            then = Effects.Destroy(permanent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Seb McKinnon"
        flavorText = "\"And then the shadow took living form, and the shadow devoured the man whole—body, " +
            "mind, and soul.\"\n—Tales of the Ozolith"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6abc24e1-e721-471a-9efd-547f320675b0.jpg?1783931058"
        ruling(
            "2020-04-17",
            "If an effect copies the Mythos spell, no mana was spent to cast the copy, so the copy won't " +
                "receive the bonus."
        )
        ruling(
            "2020-04-17",
            "Mythos of Nethroi can target any nonland permanent. Determine as the spell resolves whether " +
                "the permanent is destroyed, either because it's a creature at that time or because {G}{W} " +
                "was spent to cast the spell."
        )
        ruling(
            "2020-04-17",
            "The ability checks what mana was actually spent to cast a spell. If an effect allows you to " +
                "spend mana \"as though it were mana\" of any color or type, that allows you to spend mana " +
                "you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell."
        )
    }
}
