package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Isareth the Awakener — Core Set 2019 #104
 * {1}{B}{B} · Legendary Creature — Human Wizard · 3/3
 *
 * Deathtouch
 * Whenever Isareth attacks, you may pay {X}. When you do, return target creature card with mana
 * value X from your graveyard to the battlefield with a corpse counter on it. If that creature
 * would leave the battlefield, exile it instead of putting it anywhere else.
 *
 * The attack trigger has no target; [Effects.MayPayX] asks whether to pay, then for X, as it
 * resolves. Paying — {0} included, which returns a mana value 0 creature card — fires a reflexive
 * trigger (CR 603.12) whose target is chosen with the paid X in scope, so `manaValueEqualsX()`
 * reads it at targeting and at the resolution re-check. The returned creature gets the
 * exile-instead replacement, which outlives Isareth (ruling).
 */
val IsarethTheAwakener = card("Isareth the Awakener") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever Isareth attacks, you may pay {X}. When you do, return target creature card with mana " +
        "value X from your graveyard to the battlefield with a corpse counter on it. If that creature " +
        "would leave the battlefield, exile it instead of putting it anywhere else."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPayX(
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "return target creature card with mana value X from your graveyard " +
                    "to the battlefield with a corpse counter on it. If that creature would leave the " +
                    "battlefield, exile it instead of putting it anywhere else.",
            ) {
                val creatureCard = target(
                    TargetFilter(
                        GameObjectFilter.Creature.ownedByYou().manaValueEqualsX(),
                        zone = Zone.GRAVEYARD
                    )
                )
                effect = Effects.Move(
                    creatureCard,
                    Zone.BATTLEFIELD,
                    fromZone = Zone.GRAVEYARD,
                    addCounterType = CounterType.CORPSE,
                ) then Effects.GrantExileOnLeave(creatureCard)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74e49567-8ad6-4ce9-a55d-4db5e0fd9532.jpg?1783934568"
        ruling("2018-07-13", "Isareth's triggered ability goes on the stack without a target. While that ability is resolving, you may pay {X}. When you do, a second ability triggers and you pick a target creature card to return. This is different from abilities that say \"If you do . . .\" in that players may cast spells and activate abilities after mana is paid and the target creature card is chosen, but before that card is returned.")
        ruling("2018-07-13", "Because all attacking creatures are chosen at once, a creature returned this way can't attack during the same combat as it returns, even if it has haste.")
        ruling("2018-07-13", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-07-13", "If you somehow remove a corpse counter from a creature Isareth has returned to the battlefield, the replacement effect that will exile it continues to apply. The counter is only to help remind you which creatures will be exiled if they would leave the battlefield.")
        ruling("2018-07-13", "If Isareth leaves the battlefield, the replacement effect continues to apply. If one of the creatures it returned would leave the battlefield, it'll be exiled instead.")
        ruling("2018-07-13", "Because to die means to be put into a graveyard from the battlefield, a creature that is exiled instead doesn't \"die.\" Abilities that would trigger when it dies won't trigger.")
    }
}
