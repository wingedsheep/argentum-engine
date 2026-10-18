package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tempest Efreet
 * {1}{R}{R}{R}
 * Creature — Efreet
 * 3/3
 * Remove this card from your deck before playing if you're not playing for ante.
 * {T}, Sacrifice this creature: Target opponent may pay 10 life. If that player doesn't, they
 * reveal a card at random from their hand. Exchange ownership of the revealed card and Tempest
 * Efreet. Put the revealed card into your hand and Tempest Efreet from anywhere into that player's
 * graveyard. This change in ownership is permanent.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation).
 * - "May pay 10 life. If that player doesn't" is [Effects.PayOrSuffer]; a player with less than 10
 *   life can't pay it (CR 119.4).
 * - The exchange happens only if both halves can (CR 701.12a): an empty hand reveals nothing, and an
 *   Efreet that no longer exists (a token copy) has no ownership to trade, so then nothing changes.
 * - Each card takes the other's owner. The revealed card is handed to the Efreet's owner *before*
 *   the Efreet changes hands, since [Player.OwnerOfSource] reads its current owner.
 * - A change of owner refiles the card under its new owner without a zone change: the revealed card
 *   lands in your hand, and an Efreet in your graveyard lands in theirs. An Efreet anywhere else
 *   ("from anywhere" — exiled instead of dying, say) is then moved to its new owner's graveyard.
 * - The ownership change outlasts the game; the server settles it when the game ends.
 */
val TempestEfreet = card("Tempest Efreet") {
    manaCost = "{1}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Efreet"
    power = 3
    toughness = 3
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "{T}, Sacrifice this creature: Target opponent may pay 10 life. If that player doesn't, they reveal a " +
        "card at random from their hand. Exchange ownership of the revealed card and Tempest Efreet. Put the " +
        "revealed card into your hand and Tempest Efreet from anywhere into that player's graveyard. This " +
        "change in ownership is permanent."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val opponent = target(Targets.Opponent)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayLife(10),
            suffer = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val revealed = chooseRandom(1, from = hand)
                reveal(revealed)
                ifNotEmpty(revealed) {
                    run(Effects.If(
                        Conditions.SourceMatches(GameObjectFilter.Any),
                        Effects.ChangeOwnership(revealed.asTarget, EffectTarget.PlayerRef(Player.OwnerOfSource)) then
                            Effects.ChangeOwnership(EffectTarget.Self, opponent) then
                            Effects.If(
                                Conditions.Not(Conditions.SourceInZone(Zone.GRAVEYARD)),
                                Effects.Move(EffectTarget.Self, Zone.GRAVEYARD),
                            ),
                    ))
                }
            },
            player = opponent,
        )
        description = "{T}, Sacrifice this creature: Target opponent may pay 10 life. If that player doesn't, " +
            "they reveal a card at random from their hand. Exchange ownership of the revealed card and Tempest " +
            "Efreet. Put the revealed card into your hand and Tempest Efreet from anywhere into that player's " +
            "graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "NéNé Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb562143-fdf0-4eed-83ac-551627c576d2.jpg?1783948052"
    }
}
