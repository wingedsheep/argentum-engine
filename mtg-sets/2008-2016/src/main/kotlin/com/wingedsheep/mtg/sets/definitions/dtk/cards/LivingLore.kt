package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Living Lore — Dragons of Tarkir #61
 * {3}{U} · Creature — Avatar · Rare · * / *
 *
 * As this creature enters, exile an instant or sorcery card from your graveyard.
 * Living Lore's power and toughness are each equal to the exiled card's mana value.
 * Whenever this creature deals combat damage, you may sacrifice it. If you do, you may cast the
 * exiled card without paying its mana cost.
 *
 * The three abilities are a linked set (CR 607) over the creature's linked-exile pile:
 * - The as-enters exile is an [OnEnterRun] replacement whose pipeline links the chosen card to
 *   this permanent. It is mandatory — `chooseExactly(1)` — and an empty graveyard clamps it to
 *   nothing, so Living Lore stays a 0/0 and dies to state-based actions (ruling).
 * - The P/T is a characteristic-defining ability read through [EffectTarget.LinkedExiledCard],
 *   which only sees a card still in exile with *this* object: in any other zone, or once the card
 *   leaves exile, it reads 0 (rulings).
 * - The combat-damage trigger has no recipient restriction, so it fires once per combat damage
 *   step however many objects it hit. The sacrifice is offered on resolution and only while
 *   Living Lore is still on the battlefield; the cast is gated on the sacrifice actually
 *   happening. After the sacrifice the pile is read from the resolving ability's departed source
 *   visit, and `chooseUpTo(1)` is the "you may cast" decline. The free cast ignores timing, so a
 *   sorcery can be cast on the opponent's turn (ruling).
 */
val LivingLore = card("Living Lore") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Avatar"
    oracleText = "As this creature enters, exile an instant or sorcery card from your graveyard.\n" +
        "Living Lore's power and toughness are each equal to the exiled card's mana value.\n" +
        "Whenever this creature deals combat damage, you may sacrifice it. If you do, you may " +
        "cast the exiled card without paying its mana cost."

    // "Living Lore's power and toughness are each equal to the exiled card's mana value."
    dynamicStats(DynamicAmounts.manaValueOf(EffectTarget.LinkedExiledCard()))

    // "As this creature enters, exile an instant or sorcery card from your graveyard."
    replacementEffect(
        OnEnterRun(
            Effects.Pipeline {
                val loreCandidates = gather(
                    CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.InstantOrSorcery,
                    )
                )
                val loreExiled = chooseExactly(
                    1,
                    from = loreCandidates,
                    prompt = "Choose an instant or sorcery card to exile with Living Lore",
                    selectedLabel = "Exile",
                )
                exile(loreExiled, linkToSource = true)
            }
        )
    )

    // "Whenever this creature deals combat damage, you may sacrifice it. If you do, you may cast
    //  the exiled card without paying its mana cost."
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage()
        effect = Effects.May(
            Effects.IfYouDo(
                action = SacrificeSelfEffect,
                then = Effects.Pipeline {
                    val loreExiledCard = gather(CardSource.FromLinkedExile())
                    val loreToCast = chooseUpTo(
                        1,
                        from = loreExiledCard,
                        prompt = "You may cast the card exiled with Living Lore without paying its mana cost",
                        selectedLabel = "Cast",
                    )
                    run(Effects.CastFromCollectionWithoutPayingCost(from = loreToCast))
                },
                successCriterion = SuccessCriterion.PermanentsSacrificed,
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "sacrifice this creature. If you do, you may cast the exiled card " +
                "without paying its mana cost",
        )
        description = "Whenever this creature deals combat damage, you may sacrifice it. If you do, " +
            "you may cast the exiled card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f658281-1c6c-4450-8d75-81714dbd0472.jpg?1783938606"

        ruling("2018-12-07", "If there are no instant or sorcery cards in your graveyard as Living Lore enters the battlefield, it will enter the battlefield as a 0/0 creature and be put into its owner's graveyard due to having 0 toughness.")
        ruling("2018-12-07", "If an instant or sorcery spell puts Living Lore onto the battlefield, that spell is still on the stack while you choose a card to exile. You can't exile it with Living Lore.")
        ruling("2018-12-07", "If Living Lore leaves the battlefield, the ability that defines its power and toughness will lose track of the exiled card. Its power and toughness are 0 in other zones.")
        ruling("2018-12-07", "Similarly, if the exiled card somehow leaves exile while Living Lore is on the battlefield, it will become a 0/0 creature.")
        ruling("2018-12-07", "Living Lore's last ability will trigger whenever it deals combat damage, no matter if that damage is dealt to an opponent, an opposing planeswalker, or a creature it's blocking or being blocked by. You choose whether to sacrifice Living Lore as that ability resolves. If Living Lore has already left the battlefield, perhaps because it was dealt lethal damage at the same time it dealt damage, you can't sacrifice it.")
        ruling("2018-12-07", "You may cast a sorcery this way even if it's not your turn.")
        ruling("2018-12-07", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
