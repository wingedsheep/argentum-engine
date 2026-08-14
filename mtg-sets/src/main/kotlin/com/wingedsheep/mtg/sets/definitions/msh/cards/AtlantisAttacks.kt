package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.dsl.teamworkModal
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Atlantis Attacks — Marvel Super Heroes #46
 * {5}{U}{U} · Sorcery · Common
 *
 * Teamwork 4 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 4 or more.)
 * Choose one. If this spell was cast using teamwork, choose both instead.
 * • Target player creates a 6/5 blue Leviathan creature token with hexproof.
 * • Return one or two target nonland permanents to their owners' hands.
 *
 * The modal shape of teamwork — [teamworkModal] narrows the printed "choose both" to one mode
 * unless the teamwork cost was declared. CR 700.2 governs the mode count; the declaration it
 * branches on is made under CR 601.2b (*not* CR 702.194c, which is about targets).
 *
 * "One or two target nonland permanents" is a single requirement with `count = 2, minCount = 1`,
 * so the two chosen permanents must be different (CR 601.2c), and [ForEachTargetEffect] repeats the
 * bounce once per chosen target.
 *
 * Known deviation: if one of the two chosen permanents is illegal on resolution, the engine skips
 * this whole mode instead of bouncing the still-legal one. CR 608.2b wants the partial effect to
 * happen; `processPreChosenModeQueue` re-validates each mode all-or-nothing and says so in its own
 * comment. Pre-existing and engine-wide (`ecl/cards/WanderwineFarewell.kt` has the same exposure),
 * not specific to this card.
 */
val AtlantisAttacks = card("Atlantis Attacks") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Teamwork 4 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 4 or more.)\n" +
        "Choose one. If this spell was cast using teamwork, choose both instead.\n" +
        "• Target player creates a 6/5 blue Leviathan creature token with hexproof.\n" +
        "• Return one or two target nonland permanents to their owners' hands."

    teamwork(4)

    spell {
        teamworkModal {
            mode("Target player creates a 6/5 blue Leviathan creature token with hexproof") {
                val player = target("target player", TargetPlayer())
                effect = Effects.CreateToken(
                    power = 6,
                    toughness = 5,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Leviathan"),
                    keywords = setOf(Keyword.HEXPROOF),
                    controller = player,
                    imageUri = "https://cards.scryfall.io/normal/front/7/1/71211c95-8698-4570-abf7-3579988a329e.jpg?1783902803",
                )
            }
            mode("Return one or two target nonland permanents to their owners' hands") {
                target(
                    "one or two target nonland permanents",
                    TargetObject(count = 2, minCount = 1, filter = TargetFilter.NonlandPermanent),
                )
                effect = ForEachTargetEffect(
                    listOf(Effects.ReturnToHand(EffectTarget.ContextTarget(0))),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Alexander Skripnikov"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40bc4380-055d-4913-93cb-280c9c1d1a87.jpg?1783902962"
    }
}
