package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Brutal Expulsion (BFZ #200)
 * {2}{U}{R}
 * Instant
 * Devoid
 * Choose one or both —
 * • Return target spell or creature to its owner's hand.
 * • Brutal Expulsion deals 2 damage to target creature or planeswalker. If that creature or
 *   planeswalker would die this turn, exile it instead.
 *
 * "Choose one or both" is Winterflame's count (`chooseCount = 2, minChooseCount = 1`), with
 * Unsubstantiate's bounce as the first mode and Obliterating Bolt's damage-then-mark as the
 * second. Unlike Touch of the Void, the mark is not gated on damage actually being dealt: per the
 * ruling it applies even if the damage is prevented or redirected, and "dies" covers a
 * planeswalker too.
 *
 * The second mode is gated on its target still being on the battlefield. Both modes may name the
 * same creature; the bounce resolves first, and the card in hand is a new object (CR 400.7) that
 * the second mode can neither damage nor leave a lingering "exile it instead" mark on.
 */
val BrutalExpulsion = card("Brutal Expulsion") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Choose one or both —\n" +
        "• Return target spell or creature to its owner's hand.\n" +
        "• Brutal Expulsion deals 2 damage to target creature or planeswalker. If that creature or " +
        "planeswalker would die this turn, exile it instead."

    keywords(Keyword.DEVOID)

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Return target spell or creature to its owner's hand") {
                val t = target(TargetSpellOrPermanent(permanentFilter = GameObjectFilter.Creature))
                effect = Effects.ReturnSpellOrPermanentToOwnersHand(t)
            }
            mode("Deal 2 damage to target creature or planeswalker; if it would die this turn, exile it instead") {
                val t = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.If(
                    condition = Conditions.EntityMatches(t, GameObjectFilter.Any.onBattlefield()),
                    then = Effects.DealDamage(2, t) then Effects.MarkExileOnDeath(t),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0eb64b-a463-4f25-9278-340051139f0e.jpg?1783938182"
        ruling("2015-08-25", "If a spell is returned to its owner's hand, it's removed from the stack. The spell isn't countered, but it won't resolve. If a copy of a spell is returned to a hand this way, the copy will cease to exist the next time state-based actions are performed.")
        ruling("2015-08-25", "The second mode will exile the target creature or planeswalker if it would be put into the graveyard this turn for any reason, not just due to lethal damage. The exile effect applies to that permanent even if Brutal Expulsion deals no damage to it (due to a prevention effect) or Brutal Expulsion deals damage to a different creature or planeswalker (due to a redirection effect).")
    }
}
