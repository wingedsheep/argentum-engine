package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tectonic Giant
 * {2}{R}{R}
 * Creature — Elemental Giant
 * 3/4
 *
 * Whenever this creature attacks or becomes the target of a spell an opponent controls, choose one —
 * • This creature deals 3 damage to each opponent.
 * • Exile the top two cards of your library. Choose one of them. Until the end of your next turn,
 *   you may play that card.
 *
 * The trigger is Goldspan Dragon's `Triggers.or(attacks, becomesTarget)` narrowed to opponents'
 * spells (Swarm Shambler's `byOpponent = true, spellsOnly = true`); the mode is chosen as the
 * trigger is put on the stack. The second mode is Feldon, Ronom Excavator's exile-then-choose-one
 * pipeline with the `UntilEndOfNextTurn` expiry — the choice is made on resolution and the other
 * card stays in exile.
 */
val TectonicGiant = card("Tectonic Giant") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Giant"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature attacks or becomes the target of a spell an opponent controls, choose one —\n" +
        "• This creature deals 3 damage to each opponent.\n" +
        "• Exile the top two cards of your library. Choose one of them. Until the end of your next turn, " +
        "you may play that card."

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.self.attacks(),
            Triggers.self.becomesTarget(byOpponent = true, spellsOnly = true),
        )
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                "This creature deals 3 damage to each opponent"
            ),
            Mode.noTarget(
                Effects.Pipeline {
                    val exiled = gather(CardSource.TopOfLibrary(2))
                    exile(exiled)
                    val chosen = chooseExactly(
                        1,
                        from = exiled,
                        prompt = "Choose a card you may play until the end of your next turn"
                    )
                    run(Effects.GrantMayPlayFromExile(from = chosen, expiry = MayPlayExpiry.UntilEndOfNextTurn))
                },
                "Exile the top two cards of your library. Choose one of them. Until the end of your next turn, " +
                    "you may play that card"
            ),
            countsAsModalSpell = false
        )
        description = "Whenever this creature attacks or becomes the target of a spell an opponent controls, " +
            "choose one — 3 damage to each opponent; or exile the top two cards of your library and choose one " +
            "you may play until the end of your next turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f226550-fc9f-4964-ac62-74f84d0ac3f3.jpg?1783931544"
        ruling(
            "2020-01-24",
            "An ability that triggers when a creature becomes the target of a spell resolves before the spell " +
                "that caused it to trigger. It resolves even if that spell is countered."
        )
        ruling("2020-01-24", "If a spell targets Tectonic Giant more than once, its ability triggers only once.")
        ruling(
            "2020-01-24",
            "You choose which of the exiled cards you'll be able to play while the ability is resolving. You " +
                "can't wait to pick one later. The other card remains in exile."
        )
        ruling(
            "2020-01-24",
            "Tectonic Giant doesn't change when you can play the exiled card. For example, if you exile a sorcery " +
                "card, you can cast it only during your main phase when the stack is empty. If you exile a land " +
                "card, you can play it only during your main phase and only if you have an available land play " +
                "remaining."
        )
    }
}
