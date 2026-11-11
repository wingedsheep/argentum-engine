package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kalitas, Traitor of Ghet — Oath of the Gatewatch #86 (canonical printing).
 * {2}{B}{B} · Legendary Creature — Vampire Warrior · 3/4
 *
 * - The death shield is Draugr Necromancer's opponent-creature [RedirectZoneChangeWith] whose
 *   rider is part of the replacement (no stack, no "when you do"): the Zombie is created with the
 *   exile, before anything else happens. If Kalitas dies alongside those creatures it still
 *   applies, and you get one Zombie per card exiled (ruling).
 * - Tokens aren't matched (`nontoken`), so an opponent's token dies normally.
 * - "Sacrifice another Vampire or Zombie" excludes Kalitas itself ([Costs.SacrificeAnother]).
 */
val KalitasTraitorOfGhet = card("Kalitas, Traitor of Ghet") {
    manaCost = "{2}{B}{B}"
    typeLine = "Legendary Creature — Vampire Warrior"
    power = 3
    toughness = 4
    oracleText = "Lifelink\n" +
        "If a nontoken creature an opponent controls would die, instead exile that card and create a 2/2 black " +
        "Zombie creature token.\n" +
        "{2}{B}, Sacrifice another Vampire or Zombie: Put two +1/+1 counters on Kalitas."

    keywords(Keyword.LIFELINK)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                imageUri = "https://cards.scryfall.io/normal/front/e/0/e0bde78b-98d1-4864-899b-fc2f945ce06b.jpg?1783937860",
            ),
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.SacrificeAnother(
                GameObjectFilter.Any.withSubtype(Subtype.VAMPIRE) or
                    GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE)
            ),
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "86"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a278d0-f601-4d57-bc3d-2972f8602a69.jpg?1783937911"
        ruling("2016-01-22", "Abilities that trigger whenever a creature an opponent controls dies won't trigger unless that creature is a token (see below).")
        ruling("2016-01-22", "If a creature token an opponent controls dies, it goes to that player's graveyard as normal before ceasing to exist.")
        ruling("2016-01-22", "If Kalitas dies at the same time as creatures your opponents control, those creature cards will be exiled and you'll get that many Zombies.")
        ruling("2016-01-22", "You can't sacrifice Kalitas to activate the last ability, even if it's somehow become a Zombie.")
    }
}
