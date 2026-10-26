package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grenzo, Havoc Raiser
 * {R}{R}
 * Legendary Creature — Goblin Rogue
 * 2/2
 * Whenever a creature you control deals combat damage to a player, choose one —
 * • Goad target creature that player controls.
 * • Exile the top card of that player's library. Until end of turn, you may cast that card and you
 *   may spend mana as though it were mana of any color to cast that spell.
 *
 * "That player" is the damaged player, bound as the trigger's `Player.TriggeringPlayer` — the goad
 * target is scoped to creatures *that* player controls (not any opponent's), the same shape as
 * Fear of Burning Alive's "target creature that player controls". The exiled card is face up and
 * may only be *cast* (a land can't be played — 2016-08-23 ruling); uncast, it stays in exile.
 */
val GrenzoHavocRaiser = card("Grenzo, Havoc Raiser") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Rogue"
    power = 2
    toughness = 2
    oracleText = "Whenever a creature you control deals combat damage to a player, choose one —\n" +
        "• Goad target creature that player controls.\n" +
        "• Exile the top card of that player's library. Until end of turn, you may cast that card and " +
        "you may spend mana as though it were mana of any color to cast that spell."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = ModalEffect.chooseOne(
            mode("Goad target creature that player controls") {
                val creature = target(
                    TargetFilter(
                        GameObjectFilter.Creature.targetPlayerControls(
                            EffectTarget.PlayerRef(Player.TriggeringPlayer),
                        ),
                    ),
                )
                effect = Effects.Goad(creature)
            },
            mode(
                "Exile the top card of that player's library. Until end of turn, you may cast it, " +
                    "spending mana as though it were mana of any color"
            ) {
                effect = Effects.Pipeline {
                    val exiled = gather(CardSource.TopOfLibrary(1, player = Player.TriggeringPlayer))
                    exile(exiled, Player.TriggeringPlayer)
                    run(
                        Effects.GrantMayPlayFromExile(
                            from = exiled,
                            nonLandOnly = true,
                            withAnyManaColor = true,
                        )
                    )
                }
            },
        )
        description = "Whenever a creature you control deals combat damage to a player, choose one — " +
            "goad target creature that player controls; or exile the top card of that player's library, " +
            "and until end of turn you may cast it, spending mana as though it were mana of any color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Svetlin Velinov"
        flavorText = "\"You burn. I'll pillage.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/075cd558-2557-409a-8244-2de65664be53.jpg?1783937349"
        ruling("2016-08-23", "If you exile a land card this way, you can't play it.")
        ruling(
            "2016-08-23",
            "Casting the exiled card follows the normal rules for casting that card. You must pay its costs, " +
                "and you must follow all applicable timing rules. For example, if you exile a creature card " +
                "this way, you must wait until your main phase to cast it."
        )
        ruling("2016-08-23", "If you don't cast the card, it will remain in exile.")
    }
}
