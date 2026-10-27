package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quakebringer (Kaldheim #145)
 * {3}{R}{R} · Creature — Giant Berserker 5/4
 * Your opponents can't gain life.
 * At the beginning of your upkeep, Quakebringer deals 2 damage to each opponent. This ability
 * triggers only if Quakebringer is on the battlefield or if Quakebringer is in your graveyard and
 * you control a Giant.
 * Foretell {2}{R}{R}
 *
 * The upkeep trigger functions from two zones (CR 113.6b), so `triggerZones = {BATTLEFIELD,
 * GRAVEYARD}` — the Firemane Angel shape. Unlike the Angel's clause, "This ability triggers only
 * if …" is not an intervening-"if": it gates the trigger event only and is never re-checked on
 * resolution, so it is a [triggerRestriction]. Per the 2021-02-05 ruling, on the battlefield it
 * triggers with no Giant requirement at all (even if Quakebringer is no longer a Giant); from the
 * graveyard it needs a Giant you control at the moment the upkeep begins.
 */
val Quakebringer = card("Quakebringer") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Berserker"
    power = 5
    toughness = 4
    oracleText = "Your opponents can't gain life.\n" +
        "At the beginning of your upkeep, Quakebringer deals 2 damage to each opponent. This ability " +
        "triggers only if Quakebringer is on the battlefield or if Quakebringer is in your graveyard " +
        "and you control a Giant.\n" +
        "Foretell {2}{R}{R} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD)
        triggerRestriction = Conditions.Any(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Conditions.All(
                Conditions.SourceInZone(Zone.GRAVEYARD),
                Conditions.ControlPermanentOfType(Subtype.GIANT),
            ),
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "At the beginning of your upkeep, Quakebringer deals 2 damage to each opponent. " +
            "This ability triggers only if Quakebringer is on the battlefield or if Quakebringer is in " +
            "your graveyard and you control a Giant."
    }

    keywordAbility(KeywordAbility.foretell("{2}{R}{R}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5bbffb9-1f1c-40e5-97f4-29bf1fc68625.jpg?1783928225"
        ruling("2021-02-05", "Quakebringer entering the battlefield won't undo any life gained earlier in the turn.")
        ruling("2021-02-05", "If Quakebringer is on the battlefield at the beginning of your upkeep, its ability will trigger even if it's no longer a Giant and you don't control another Giant.")
        ruling("2021-02-05", "Because exiling a card with foretell from your hand is a special action, you can do so any time you have priority during your turn, including in response to spells and abilities. Once you announce you're taking the action, no other player can respond by trying to remove the card from your hand.")
        ruling("2021-02-05", "Casting a foretold card from exile follows the timing rules for that card. If you foretell an instant card, you can cast it as soon as the next player's turn. In most cases, if you foretell a card that isn't an instant (or doesn't have flash), you'll have to wait until your next turn to cast it.")
        ruling("2021-02-05", "If you're casting a foretold card from exile for its foretell cost, you can't choose to cast it for any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
    }
}
