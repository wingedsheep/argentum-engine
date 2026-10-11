package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Derevi, Empyrial Tactician
 * {G}{W}{U}
 * Legendary Creature — Bird Wizard
 * 2/3
 * Flying
 * When Derevi enters and whenever a creature you control deals combat damage to a player, you may
 * tap or untap target permanent.
 * {1}{G}{W}{U}: Put Derevi onto the battlefield from the command zone.
 *
 * The one printed ability with two trigger conditions is two triggered abilities sharing an effect —
 * the conditions can never fire on the same event, so splitting them changes nothing. The effect is
 * the [com.wingedsheep.mtg.sets.definitions.lrw.cards.Pestermite] idiom: target chosen when the
 * trigger goes on the stack, tap-or-untap chosen on resolution, under a "may".
 *
 * The last ability functions only from the command zone (`activateFromZone = Zone.COMMAND`,
 * enumerated by `CommandZoneAbilityEnumerator`). It is an ability, not a cast: no commander tax, and
 * it can't be countered as a spell. The move carries `fromZone = COMMAND` so a Derevi that left the
 * command zone in response (e.g. was cast) isn't put onto the battlefield from elsewhere.
 */
val DereviEmpyrialTactician = card("Derevi, Empyrial Tactician") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Creature — Bird Wizard"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When Derevi enters and whenever a creature you control deals combat damage to a player, " +
        "you may tap or untap target permanent.\n" +
        "{1}{G}{W}{U}: Put Derevi onto the battlefield from the command zone."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(permanent), "Tap that permanent"),
                    Mode.noTarget(Effects.Untap(permanent), "Untap that permanent")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
        description = "When Derevi enters, you may tap or untap target permanent."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(permanent), "Tap that permanent"),
                    Mode.noTarget(Effects.Untap(permanent), "Untap that permanent")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
        description = "Whenever a creature you control deals combat damage to a player, " +
            "you may tap or untap target permanent."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}{W}{U}")
        activateFromZone = Zone.COMMAND
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.COMMAND)
        description = "Put Derevi onto the battlefield from the command zone"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d61a4d4-f0d5-4bc4-8977-57bbb3d97776.jpg?1783939652"
        ruling("2020-11-10", "You can activate Derevi's last ability only when it is in the command zone.")
        ruling(
            "2020-11-10",
            "When you activate Derevi's last ability, you're not casting Derevi as a spell. The ability can't " +
                "be countered by something that counters only spells. The ability isn't subject to the " +
                "commander tax, nor does it increase the tax if you cast Derevi from the command zone later in the game."
        )
    }
}
