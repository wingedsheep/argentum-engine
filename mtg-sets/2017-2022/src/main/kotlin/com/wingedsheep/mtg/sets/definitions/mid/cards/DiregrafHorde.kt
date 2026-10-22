package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Diregraf Horde
 * {4}{B}
 * Creature — Zombie
 * 3/4
 * When this creature enters, create two 2/2 black Zombie creature tokens with decayed. When you do,
 * exile up to two target cards from graveyards.
 *
 * The "when you do" is a reflexive trigger (CR 603.12) whose action is the token creation itself
 * (`optional = false` — the creation isn't a choice). Its "up to two target cards from graveyards"
 * are chosen when the reflexive trigger goes on the stack, after the tokens exist, and each chosen
 * card is exiled by `ForEachTarget`. Unlike Griffnaut Tracker, the targets may come from different
 * graveyards.
 */
val DiregrafHorde = card("Diregraf Horde") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, create two 2/2 black Zombie creature tokens with decayed. " +
        "When you do, exile up to two target cards from graveyards. (A creature with decayed can't " +
        "block. When it attacks, sacrifice it at end of combat.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                keywords = setOf(Keyword.DECAYED),
                imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
                count = 2,
            ),
            optional = false,
            reflexiveEffect = Effects.ForEachTarget(
                Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(filter = TargetFilter.CardInGraveyard, count = 2, optional = true)
            ),
            descriptionOverride = "Create two 2/2 black Zombie creature tokens with decayed. When you do, " +
                "exile up to two target cards from graveyards.",
        )
        description = "When this creature enters, create two 2/2 black Zombie creature tokens with decayed. " +
            "When you do, exile up to two target cards from graveyards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Alex Negrea"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/153be768-ddad-44f2-bcdd-c40353c807d7.jpg?1783925620"
    }
}
