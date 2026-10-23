package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gisa, Glorious Resurrector — Innistrad: Midnight Hunt #103
 * {2}{B}{B} · Legendary Creature — Human Wizard · 4/4
 *
 * If a creature an opponent controls would die, exile it instead.
 * At the beginning of your upkeep, put all creature cards exiled with Gisa onto the battlefield
 * under your control. They gain decayed.
 *
 * Modeling notes:
 *  - The replacement is [RedirectZoneChange] battlefield → graveyard → exile for creatures an
 *    opponent controls, with `linkToSource = true` so each exiled card joins Gisa's linked-exile
 *    pile (CR 607 linked abilities). A creature exiled by some *other* replacement is never linked,
 *    so it doesn't come back (per the 2021-09-24 ruling).
 *  - The upkeep trigger gathers that pile ([CardSource.FromLinkedExile]), moves only the creature
 *    cards onto the battlefield under your control, and grants each one that actually arrived the
 *    real [Keyword.DECAYED] keyword for as long as it stays there ([Duration.Permanent]). The engine
 *    drives decayed off the projected keyword — "can't block" plus the attack-triggered end-of-combat
 *    sacrifice — so a granted decayed behaves exactly like a printed one.
 */
val GisaGloriousResurrector = card("Gisa, Glorious Resurrector") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 4
    toughness = 4
    oracleText = "If a creature an opponent controls would die, exile it instead.\n" +
        "At the beginning of your upkeep, put all creature cards exiled with Gisa onto the " +
        "battlefield under your control. They gain decayed. (A creature with decayed can't block. " +
        "When it attacks, sacrifice it at end of combat.)"

    // If a creature an opponent controls would die, exile it instead — linked to Gisa.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            linkToSource = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    // At the beginning of your upkeep, return the exiled creature cards under your control with decayed.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val returned = moveTracked(
                exiled,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                filter = GameObjectFilter.Creature,
            )
            run(
                Effects.ForEachInCollection(
                    returned,
                    Effects.GrantKeyword(Keyword.DECAYED, EffectTarget.IterationEntity, Duration.Permanent),
                )
            )
        }
        description = "At the beginning of your upkeep, put all creature cards exiled with Gisa " +
            "onto the battlefield under your control. They gain decayed."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63461076-a13d-488d-bd82-ceac441341c8.jpg?1783925617"
        ruling("2021-09-24", "Gisa does not grant haste to the creatures that are returned to the battlefield, so you normally won't be able to attack with them the turn they are returned.")
        ruling("2021-09-24", "Because creatures controlled by opponents aren't dying due to Gisa's replacement effect, any \"when [this creature] dies\" triggered abilities those creatures have won't trigger.")
        ruling("2021-09-24", "Once a creature with decayed attacks, it will be sacrificed at end of combat, even if it no longer has decayed at that time.")
        ruling("2021-09-24", "If a creature your opponent controls would die and more than one effect would cause it to be exiled, that opponent chooses which one to apply. If the creature is exiled due to some other replacement effect, it will not be returned to the battlefield with Gisa.")
    }
}
