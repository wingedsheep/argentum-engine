package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val Vault11VotersDilemma = card("Vault 11: Voter's Dilemma") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\nI — For each opponent, you create a 1/1 white Human Soldier creature token.\nII, III — Each player secretly votes for up to one creature, then those votes are revealed. If no creature got votes, each player draws a card. Otherwise, destroy each creature with the most votes or tied for most votes."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCount(Player.EachOpponent),
            power = 1, toughness = 1, colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/7/3/737741e7-b2b2-4c41-ab7c-4eeaf9f37b71.jpg?1783912046",
        )
    }
    for (chapter in 2..3) {
        sagaChapter(chapter) {
            effect = Effects.Pipeline {
                val creatures = gather(GameObjectFilter.Creature)
                val winners = runStoringCollection { key ->
                    Effects.SecretEntities(from = creatures, storeMostChosen = key)
                }
                ifNotEmpty(winners) {
                    move(winners, CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Destroy)
                } orElse {
                    run(Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each)))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bafcd4e-5693-4c23-be79-75653fcd214c.jpg?1783912383"
        ruling("2024-03-08", "To secretly vote, each player writes down their chosen option without showing it to anyone else. Each player then keeps their vote secret until all players simultaneously reveal their votes.")
        ruling("2024-03-08", "Before secret votes are revealed, players may announce how they intend to vote, but they can't reveal what they actually wrote down until all votes are simultaneously revealed. Players can lie about how they intend to vote before the votes are revealed.")
        ruling("2024-03-08", "Abilities that trigger \"whenever players finish voting\" trigger once all players have voted or once all secret votes are revealed, but they won't go on the stack until the current spell or ability finishes resolving.")
        ruling("2024-03-08", "No player votes until Vault 11's last ability resolves. Any responses to the ability must be made without knowing the outcome of the vote.")
    }
}
