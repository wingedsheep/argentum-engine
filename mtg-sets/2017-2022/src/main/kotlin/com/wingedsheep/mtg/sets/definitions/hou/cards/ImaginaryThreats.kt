package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Imaginary Threats — both clauses bind the target opponent's creatures as a live group, not a
 * snapshot: the attack requirement is a rules modification over "creatures that player controls"
 * for the turn (only the player is fixed at resolution), and per the rulings every creature they
 * control at their next untap step stays tapped, including ones that entered or became tapped
 * after the spell resolved, and ones that didn't attack.
 */
val ImaginaryThreats = card("Imaginary Threats") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Creatures target opponent controls attack this turn if able. During that player's next untap step, " +
        "creatures they control don't untap.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.MustAttackGroup(GroupFilter(GameObjectFilter.Creature.targetOpponentControls())) then
            Effects.SkipUntap(target = opponent, permanents = GameObjectFilter.Creature)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Magali Villeneuve"
        flavorText = "Liliana always knew Jace's illusions would come in handy."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c3a6a5-9a50-4a38-9f5f-b8caa320909d.jpg?1783936052"
        ruling("2017-07-14", "No creatures that player controls will untap during their next untap step, even creatures that don't attack. This includes creatures that enter the battlefield or become tapped after this spell resolves.")
        ruling("2017-07-14", "If the opponent exerts any creatures they control, exert and the effect from Imaginary Threats stopping them from untapping both apply in the same untap step. Those creatures will untap as normal in the player's subsequent untap step.")
        ruling("2017-07-14", "If, during that player's declare attackers step, a creature that player controls is tapped or is affected by a spell or ability that says it can't attack, then it doesn't attack. If there's a cost associated with having a creature attack, its controller isn't forced to pay that cost, so it doesn't have to attack in that case either.")
    }
}
